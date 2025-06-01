package com.gardensmc.gardensfurniture.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;


public class ProtectionUtil {

    public static boolean canBuild(Location location, Player player) {
        var block = location.getBlock();
        var blockData = block.getBlockData();
        var buildEvent = new BlockPlaceEvent(
                block,
                block.getState(),
                block.getRelative(BlockFace.DOWN),
                player.getInventory().getItemInMainHand(),
                player,
                true,
                EquipmentSlot.HAND
        );
        Bukkit.getPluginManager().callEvent(buildEvent);
        // prevent physics update
        block.setBlockData(blockData, false);
        return !buildEvent.isCancelled();
    }
}
