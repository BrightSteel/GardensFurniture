package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.loader.ChunkFurnitureLoader;
import com.gardensmc.gardensfurniture.store.FurnitureCache;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ChunkListener implements Listener {

    @EventHandler
    public void onLoadChunk(ChunkLoadEvent e) {
        // load chunk to store instead of waiting for first packet sent - this should speed up initial waiting period
        FurnitureCache.INSTANCE.getFurnitureStore(new FurnitureCache.ChunkKey(e.getChunk()), new FurnitureCache.BlockKey(0, 0, 0));
        if (e.isNewChunk()) {
            var snapshot = e.getChunk().getChunkSnapshot();
            Bukkit.getScheduler().runTaskAsynchronously(
                    GardensFurniture.plugin,
                    () -> ChunkFurnitureLoader.INSTANCE.processSnapshot(snapshot)
            );
        }
    }
}
