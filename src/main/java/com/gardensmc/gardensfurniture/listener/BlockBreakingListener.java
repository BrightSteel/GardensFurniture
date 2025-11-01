package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.breaking.CustomBreakingBlock;
import com.gardensmc.gardensfurniture.breaking.SoundBreakingBlock;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.listener.sounds.VanillaBlockSoundListener;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockBreakingListener implements Listener {

    public static final Map<UUID, CustomBreakingBlock> customBreakingBlocks = new HashMap<>();
    public static final Map<Block, SoundBreakingBlock> soundBreakingBlocks = new HashMap<>();

    @EventHandler
    public void onBeginBreakCustomBlock(BlockDamageEvent e) {
        if (e.getInstaBreak()) {
            return;
        }
        var block = e.getBlock();
        var customItem = CustomBlockManager.getCustomItemFromBlock(block);
        if (!(customItem instanceof CustomPlaceableItem placeable)) {
            return;
        }
        var hardnessMaterial = placeable.getCustomBlock().getMaterialTraits();
        if (hardnessMaterial == null) {
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

    @EventHandler
    public void onCancelBreak(BlockDamageAbortEvent e) {
        removeBreaking(e.getPlayer(), e.getBlock());
    }

    @EventHandler
    public void onFinishBreak(BlockBreakEvent e) {
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

}
