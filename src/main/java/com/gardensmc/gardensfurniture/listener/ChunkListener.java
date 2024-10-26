package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.loader.ChunkFurnitureLoader;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ChunkListener implements Listener {

    @EventHandler
    public void onLoadChunk(ChunkLoadEvent e) {
        if (e.isNewChunk()) {
            var snapshot = e.getChunk().getChunkSnapshot();
            Bukkit.getScheduler().runTaskAsynchronously(
                    GardensFurniture.plugin,
                    () -> ChunkFurnitureLoader.INSTANCE.processSnapshot(snapshot)
            );
        }
    }
}
