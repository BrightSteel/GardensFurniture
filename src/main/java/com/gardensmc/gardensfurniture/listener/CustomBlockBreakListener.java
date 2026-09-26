package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import com.gardensmc.gardensfurniture.events.CustomBlockBreakEvent;
import com.gardensmc.gardensfurniture.events.FurnitureBreakEvent;
import com.gardensmc.gardensfurniture.util.LightUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class CustomBlockBreakListener implements Listener {

    // MONITOR: only once no other listener can still cancel the break
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(CustomBlockBreakEvent e) {
        removeLight(e.getBlock().getLocation(), e.getCustomItem().getCustomBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnitureBreak(FurnitureBreakEvent e) {
        removeLight(e.getItemEntity().getEntity().getLocation(), e.getCustomItem().getCustomBlock());
    }

    // real block break or custom block - replace light if needed
    @EventHandler(ignoreCancelled = true)
    public void onBlockCoveringLightBreak(BlockBreakEvent e) {
        Bukkit.getScheduler().runTaskLater(GardensFurniture.plugin, () -> {
            // run later or else it just gets removed
            LightUtil.addLightBack(e.getBlock());
        }, 1);
    }

    private void removeLight(Location location, CustomBlock customBlock) {
        customBlock.removeLight(location);
    }
}
