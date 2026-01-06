package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.breaking.CustomBreakingBlock;
import com.gardensmc.gardensfurniture.breaking.SoundBreakingBlock;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.listener.sounds.VanillaBlockSoundListener;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Tool;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import io.papermc.paper.registry.set.RegistryKeySet;
import net.kyori.adventure.util.TriState;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.SoundGroup;
import org.bukkit.block.Block;
import org.bukkit.block.BlockType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BlockBreakingListener implements Listener {

    public static final Map<UUID, CustomBreakingBlock> customBreakingBlocks = new HashMap<>();
    public static final Map<Block, SoundBreakingBlock> soundBreakingBlocks = new HashMap<>();

    // not sure if a better way of identifying all wood blocks
    private static final Set<SoundGroup> WOOD_SOUND_GROUPS = Set.of(
            Material.NOTE_BLOCK.createBlockData().getSoundGroup(),
            Material.CHERRY_LOG.createBlockData().getSoundGroup(),
            Material.BAMBOO_PLANKS.createBlockData().getSoundGroup(),
            Material.WARPED_STEM.createBlockData().getSoundGroup()
    );

    @EventHandler
    public void onBeginBreakCustomBlock(BlockDamageEvent e) {
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }
        var block = e.getBlock();
        Material hardnessMaterial;
        if (CustomBlockManager.getCustomItemFromBlock(block) instanceof CustomPlaceableItem placeable) {
            hardnessMaterial = placeable.getCustomBlock().getMaterialTraits();
        } else if (e.getItemInHand().getType() != Material.AIR && WOOD_SOUND_GROUPS.contains(block.getBlockSoundGroup())) {
            hardnessMaterial = block.getType();
        } else {
            return;
        }
        var player = e.getPlayer();
        var heldItem = e.getItemInHand();
        var playerUUID = player.getUniqueId();
        customBreakingBlocks.put(playerUUID, new CustomBreakingBlock(player, block, heldItem, hardnessMaterial));
    }

    @EventHandler
    public void onBeginBreakBlock(BlockDamageEvent e) {
        if (e.getInstaBreak()) {
            return;
        }
        var soundOverride = VanillaBlockSoundListener.getSoundToPlay(e.getBlock(), "hit");
        if (soundOverride != null && !soundBreakingBlocks.containsKey(e.getBlock())) {
            soundBreakingBlocks.put(e.getBlock(), new SoundBreakingBlock(e.getBlock(), soundOverride));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerLogin(PlayerJoinEvent e) {
        updateToolRule(e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemChange(EntityEquipmentChangedEvent e) {
        if (e.getEntity() instanceof Player player && e.getEquipmentChanges().containsKey(EquipmentSlot.HAND)) {
            updateToolRule(player);
        }
    }

    private void updateToolRule(Player player) {
        var inv = player.getInventory();
        var slot = inv.getHeldItemSlot();
        var item = inv.getItem(slot);
        if (item != null && item.getType() != Material.AIR) {
            // modify new item to break noteblocks slowly
            var heldClone = item.clone();
            heldClone.editMeta(meta -> {
                var tool = meta.getTool();
                tool.addRule(Material.NOTE_BLOCK, 0.00001f, false);
                meta.setTool(tool);
            });
            sendUpdateItemPacket(player, slot, heldClone);
        }
    }

    private void sendUpdateItemPacket(Player player, int slot, ItemStack itemStack) {
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, new WrapperPlayServerSetPlayerInventory(
                slot,
                SpigotConversionUtil.fromBukkitItemStack(itemStack)
        ));
    }

    @EventHandler
    public void onCancelBreak(BlockDamageAbortEvent e) {
        removeBreaking(e.getPlayer(), e.getBlock());
    }

    @EventHandler
    public void onFinishBreak(BlockBreakEvent e) {
        if (isStillBreaking(e.getPlayer(), e.getBlock())) {
            e.setCancelled(true);
            return;
        }
        removeBreaking(e.getPlayer(), e.getBlock());
    }

//    @EventHandler
//    public void onPlayerQuit(PlayerQuitEvent e) {
//        removeBreaking(e.getPlayer());
//    }
//
//    @EventHandler
//    public void onPlayerTeleport(PlayerTeleportEvent e) {
//        removeBreaking(e.getPlayer());
//    }
//
//    @EventHandler
//    public void onPlayerChangeWorld(PlayerChangedWorldEvent e) {
//        removeBreaking(e.getPlayer());
//    }

    private void removeBreaking(Player player, Block block) {
        var breakingBlock = customBreakingBlocks.get(player.getUniqueId());
        if (breakingBlock != null) {
            breakingBlock.close();
        }
        var soundBreakingBlock = soundBreakingBlocks.get(block);
        if (soundBreakingBlock != null) {
            soundBreakingBlock.close();
        }
    }

    private boolean isStillBreaking(Player player, Block block) {
        var breakingBlock = customBreakingBlocks.get(player.getUniqueId());
        return breakingBlock != null && breakingBlock.isBreakingInProgress();
    }

}
