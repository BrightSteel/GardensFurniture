package com.gardensmc.gardensfurniture.breaking;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.item.mechanics.AirborneBreakPenaltyHandler;
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
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public class CustomBreakingBlock {

    private static final int INVISIBLE_LEAVES_ID = getBreakBlockId();
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
        if (ticksPassed == 0 && isBarrier && !isInstantBreak()) {
            // A barrier isn't diggable, so the client would never render a crack overlay on it. Swap it
            // (for this player only) to the invisible break block so the vanilla client shows the breaking
            // animation; our tick loop drives the actual stages/speed. The break block's type is in the
            // tool-rule suppression list, so the client can't dig it out from under the animation - which
            // is why no Mining Fatigue is needed here. Skip the swap for an instant break (would just flicker).
            sendBlockChangePacket(player, INVISIBLE_LEAVES_ID);
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

    // A single-tick break time is effectively instant - no crack animation is worth showing,
    // so we skip the barrier -> break-block swap for it.
    private boolean isInstantBreak() {
        return breakTimeInTicks <= 1;
    }

    // True once this break is showing (or should show) the invisible break block instead of the
    // barrier, so BlockPacketListener can keep rewriting server barrier re-sends back to the leaf.
    public boolean isSwappedToBreakBlock() {
        return isBarrier && !isInstantBreak();
    }

    public boolean isAtBlock(int x, int y, int z) {
        return block.getX() == x && block.getY() == y && block.getZ() == z;
    }

    // The client-side block a broken barrier is shown as (invisible in the resource pack) so the
    // crack overlay has geometry to render on.
    public static int getInvisibleBreakBlockId() {
        return INVISIBLE_LEAVES_ID;
    }

    public void close() {
        task.cancel();
        BlockBreakingListener.customBreakingBlocks.remove(playerUUID);
        var player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            sendBlockDestroyStagePacket(player, (byte) 10); // invalid stage to remove animation
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

    public static int calculateBreakTimeInTicks(Player player, ItemStack heldItem, Material hardnessMaterial) {
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

    private static double getSpeedMultiplier(Player player, ItemStack heldItem, Material blockMaterial) {
        BlockState blockState = ((CraftBlockState) blockMaterial.createBlockData().createBlockState()).getHandle();
        // Base tool speed straight from vanilla, so it matches what the client itself predicts and correctly
        // handles tools the "correct tool for drops" check misses - e.g. shears/hoe on leaves, which mine
        // fast but aren't a drop-gating tool (leaves drop with anything).
        double multiplier = CraftItemStack.asNMSCopy(heldItem).getDestroySpeed(blockState);
        if (multiplier > 1) {
            var efficiencyLevel = heldItem.getEnchantmentLevel(Enchantment.EFFICIENCY);
            if (efficiencyLevel > 0) {
                // vanilla: digSpeed += efficiency^2 + 1 (only when the tool is actually effective, i.e. speed > 1)
                multiplier += efficiencyLevel * efficiencyLevel + 1;
            }
        }
        var hasteEffect = player.getPotionEffect(PotionEffectType.HASTE);
        if (hasteEffect != null) {
            // vanilla: digSpeed *= 1 + 0.2 * hasteLevel, where hasteLevel = amplifier + 1
            // (getAmplifier() is 0-based, so Haste I -> amplifier 0).
            multiplier *= 1 + 0.2 * (hasteEffect.getAmplifier() + 1);
        }
        if (player.isInWater() && !heldItem.containsEnchantment(Enchantment.AQUA_AFFINITY)) {
            multiplier /= 5;
        }
        // The disable_airborn_break_penalty mechanic cancels the vanilla airborne slowdown; the client-side
        // equivalent for normal blocks is handled by AirborneBreakPenaltyHandler.
        if (!player.isOnGround() && !AirborneBreakPenaltyHandler.hasMechanic(heldItem)) {
            multiplier /= 5;
        }
        return multiplier;
    }

    private static boolean isPreferredTool(ItemStack heldItem, Material blockMaterial) {
        BlockState blockState = ((CraftBlockState) blockMaterial.createBlockData().createBlockState()).getHandle();
        var item = CraftItemStack.asNMSCopy(heldItem);
        return item.isCorrectToolForDrops(blockState);
    }

    private static boolean isCanHarvest(ItemStack heldItem, Material blockMaterial) {
        var blockData = ((CraftBlockData) blockMaterial.createBlockData());
        return !blockData.requiresCorrectToolForDrops() || isPreferredTool(heldItem, blockMaterial);
    }

    // The block a barrier is (client-side only) swapped to while it's being broken, so the vanilla
    // client has a real, diggable surface to render the crack overlay on. Leaves are used rather than
    // tripwire because their non-zero hardness lets Mining Fatigue actually slow the client-side dig,
    // keeping it in sync with our server-driven break stages. Birch leaves at distance 6 / persistent
    // false is the state the resource pack renders invisibly.
    private static int getBreakBlockId() {
        var state = StateTypes.BIRCH_LEAVES.createBlockState();
        state.setDistance(6);
        state.setPersistent(false);
        return state.getGlobalId();
    }
}
