package com.gardensmc.gardensfurniture.breaking;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.listener.BlockBreakingListener;
import com.gardensmc.gardensfurniture.listener.sounds.VanillaBlockSoundListener;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public class CustomBreakingBlock {

//    private static final int INVISIBLE_LEAVES_ID = getBreakBlockId();
    private static final int BARRIER_ID = StateTypes.BARRIER.createBlockState().getGlobalId();

    private final UUID playerUUID;
    private final Block block;
    private final int breakTimeInTicks;
    private final BukkitTask task;
    private final int entityId;
    private final Material materialTraits;
    private final boolean isBarrier;

    private int ticksPassed;
    @Getter
    private byte lastStageSent = -1;
    private final ItemStack heldItem;

    public CustomBreakingBlock(Player player, Block block, ItemStack heldItem, Material materialTraits) {
        this.playerUUID = player.getUniqueId();
        this.block = block;
        this.breakTimeInTicks = calculateBreakTimeInTicks(player, heldItem, materialTraits);
        this.task = Bukkit.getScheduler().runTaskTimer(
                GardensFurniture.plugin, this::tick, 0, 1
        );
        this.entityId = ((block.getX() & 0xFFF) << 20) | ((block.getY() & 0xFFF) << 8) | (block.getZ() & 0xFF); // unique id
        this.materialTraits = materialTraits;
        this.isBarrier = block.getType() == Material.BARRIER;
        this.heldItem = heldItem;
    }

    private void tick() {
        var player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            close();
            return;
        }
        if (ticksPassed == 0) {
            if (heldItem == null || heldItem.getType() == Material.AIR) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.MINING_FATIGUE, breakTimeInTicks, 1, false, false, false
                ));
            }

//            if (isBarrier) {
//                sendBlockChangePacket(player, INVISIBLE_LEAVES_ID); // invisible block for breaking animation to show
//            }
        }
        ticksPassed++;
        byte currStage = (byte) Math.floor(((double) ticksPassed / breakTimeInTicks) * 9.0d); // convert into stage from 0-9
        if (currStage > lastStageSent) {
            lastStageSent = currStage;
            sendBlockDestroyStagePacket(player, currStage);
        }
        if (ticksPassed == breakTimeInTicks) {
            breakBlock();
        }
    }

    public boolean isBreakingInProgress() {
        return ticksPassed < breakTimeInTicks;
    }

    public void close() {
        task.cancel();
        BlockBreakingListener.customBreakingBlocks.remove(playerUUID);
        var player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            sendBlockDestroyStagePacket(player, (byte) 10); // invalid stage to remove animation
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            if (isBarrier) {
                sendBlockChangePacket(player, BARRIER_ID); // revert to barrier
            }
        }
    }

    private void breakBlock() {
        close();
        var player = Bukkit.getPlayer(playerUUID);
        if (player == null || block == null) return;
        block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 100, 0.1, 0.1, 0.1, 4, block.getBlockData());
        var sound = getBlockSound();
        if (sound != null) {
            block.getWorld().playSound(block.getLocation(), sound, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
        CustomBlockManager.breakFurniture(player, block.getLocation(), true);
        ((CraftPlayer) player).getHandle().gameMode.destroyBlock(new BlockPos(block.getX(), block.getY(), block.getZ()));
    }

    // null if vanilla block that isn't one we need to play a sound for, but still do breaking (like cherry wood and bamboo)
    @Nullable
    private String getBlockSound() {
        return VanillaBlockSoundListener.getSoundToPlay(block, "break");
    }

    private void sendBlockDestroyStagePacket(Player player, byte stage) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerBlockBreakAnimation(
                entityId,
                new Vector3i(block.getX(), block.getY(), block.getZ()),
                stage
        ));
    }

    private void sendBlockChangePacket(Player player, int blockId) {
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, new WrapperPlayServerBlockChange(
                new Vector3i(block.getX(), block.getY(), block.getZ()),
                blockId
        ));
    }

    private int calculateBreakTimeInTicks(Player player, ItemStack heldItem, Material hardnessMaterial) {
        var hardness = hardnessMaterial.getHardness();
        var speedMultiplier = getSpeedMultiplier(player, heldItem, hardnessMaterial);
        double damage = speedMultiplier / hardness;
        if (isCanHarvest(heldItem, hardnessMaterial)) {
            damage /= 30;
        } else {
            damage /= 100;
        }
        return damage > 1 ? 1 : (int) Math.ceil(1 / damage);
    }

    private double getSpeedMultiplier(Player player, ItemStack heldItem, Material blockMaterial) {
        double multiplier = 1;
        if (isPreferredTool(heldItem, blockMaterial)) {
            multiplier = getToolMultiplier(heldItem.getType(), blockMaterial);
            var efficiencyLevel = heldItem.getEnchantmentLevel(Enchantment.EFFICIENCY);
            if (efficiencyLevel > 0) {
                multiplier += efficiencyLevel ^ 2 + 1;
            }
        }
        var hasteEffect = player.getPotionEffect(PotionEffectType.HASTE);
        if (hasteEffect != null) {
            multiplier *= 0.2 * hasteEffect.getAmplifier() + 1;
        }
        if (player.isInWater() && !heldItem.containsEnchantment(Enchantment.AQUA_AFFINITY)) {
            multiplier /= 5;
        }
        if (!player.isOnGround()) {
            multiplier /= 5;
        }
        return multiplier;
    }

    private boolean isPreferredTool(ItemStack heldItem, Material blockMaterial) {
        BlockState blockState = ((CraftBlockState) blockMaterial.createBlockData().createBlockState()).getHandle();
        var item = CraftItemStack.asNMSCopy(heldItem);
        return item.isCorrectToolForDrops(blockState);
    }

    private boolean isCanHarvest(ItemStack heldItem, Material blockMaterial) {
        var blockData = ((CraftBlockData) blockMaterial.createBlockData());
        return !blockData.requiresCorrectToolForDrops() || isPreferredTool(heldItem, blockMaterial);
    }

    // Uses code from https://git.mythiccraft.io/ASangarin/breaker/
    public static double getToolMultiplier(Material type, Material block) {
        return switch (type) {
            case WOODEN_SWORD, GOLDEN_SWORD, STONE_SWORD, IRON_SWORD, DIAMOND_SWORD, NETHERITE_SWORD -> block == Material.COBWEB ? 15 : 1.5;

            case WOODEN_AXE, WOODEN_HOE, WOODEN_SHOVEL, WOODEN_PICKAXE -> 2;
            case GOLDEN_AXE, GOLDEN_HOE, GOLDEN_SHOVEL, GOLDEN_PICKAXE -> 12;
            case STONE_AXE, STONE_HOE, STONE_SHOVEL, STONE_PICKAXE -> 4;
            case COPPER_AXE, COPPER_HOE, COPPER_SHOVEL, COPPER_PICKAXE -> 5;
            case IRON_AXE, IRON_HOE, IRON_SHOVEL, IRON_PICKAXE -> 6;
            case DIAMOND_AXE, DIAMOND_HOE, DIAMOND_SHOVEL, DIAMOND_PICKAXE -> 8;
            case NETHERITE_AXE, NETHERITE_HOE, NETHERITE_SHOVEL, NETHERITE_PICKAXE -> 9;

            case SHEARS -> calculateShears(block);

            default -> 1;
        };
    }

    public static double calculateShears(Material block) {
        return switch (block) {
            case VINE, CAVE_VINES, GLOW_LICHEN -> 1;
            case WHITE_WOOL, BLACK_WOOL, BLUE_WOOL, BROWN_WOOL, CYAN_WOOL, GRAY_WOOL, GREEN_WOOL, LIGHT_BLUE_WOOL,
                 LIGHT_GRAY_WOOL, LIME_WOOL, MAGENTA_WOOL, ORANGE_WOOL, PINK_WOOL, PURPLE_WOOL, RED_WOOL, YELLOW_WOOL ->
                    5;
            case COBWEB, ACACIA_LEAVES, AZALEA_LEAVES, BIRCH_LEAVES, DARK_OAK_LEAVES,
                 FLOWERING_AZALEA_LEAVES, JUNGLE_LEAVES, MANGROVE_LEAVES, OAK_LEAVES, SPRUCE_LEAVES ->
                    15;
            default -> 2;
        };
    }

    private static int getBreakBlockId() {
        return StateTypes.TRIPWIRE.createBlockState().getGlobalId();
    }
}
