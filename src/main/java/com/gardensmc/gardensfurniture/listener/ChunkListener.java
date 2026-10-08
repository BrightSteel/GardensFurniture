package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.loader.ChunkFurnitureLoader;
import com.gardensmc.gardensfurniture.store.FurnitureCache;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkListener implements Listener {

    @EventHandler
    public void onLoadChunk(ChunkLoadEvent e) {
        var environment = e.getWorld().getEnvironment();
        if (environment != World.Environment.NETHER && GardensFurniture.plugin.getConfig().getBoolean("world_gen_furniture.enabled", true)) {
            if (e.isNewChunk()) {
                var snapshot = e.getChunk().getChunkSnapshot();
                int minY = e.getWorld().getMinHeight(), maxY = e.getWorld().getMaxHeight();
                var world = e.getWorld().getName();
                // counted until its blocks are queued, so ChunkFurnitureLoader.pending sees work not yet queued
                ChunkFurnitureLoader.INSTANCE.scanStarted(world);
                Bukkit.getScheduler().runTaskAsynchronously(GardensFurniture.plugin, () -> {
                    try {
                        ChunkFurnitureLoader.INSTANCE.processSnapshot(snapshot, minY, maxY);
                    } finally {
                        ChunkFurnitureLoader.INSTANCE.scanFinished(world);
                    }
                });
            }
        }
    }

    // keep furniture data cached for exactly the loaded chunks, so packet threads never have to load it
    @EventHandler
    public void cacheChunkFurniture(ChunkLoadEvent e) {
        FurnitureCache.INSTANCE.loadChunk(new FurnitureCache.ChunkKey(e.getChunk()));
    }

    // an autosave writes chunks without unloading them: buffered world-gen stores go in first
    @EventHandler
    public void flushOnSave(org.bukkit.event.world.WorldSaveEvent e) {
        com.gardensmc.gardensfurniture.store.FurnitureStoreHandler.INSTANCE.flushAll(e.getWorld());
    }

    @EventHandler
    public void uncacheChunkFurniture(ChunkUnloadEvent e) {
        // before it's saved: buffered world-gen stores into its data
        com.gardensmc.gardensfurniture.store.FurnitureStoreHandler.INSTANCE.flush(e.getChunk());
        FurnitureCache.INSTANCE.invalidateChunk(new FurnitureCache.ChunkKey(e.getChunk()));
    }
}
