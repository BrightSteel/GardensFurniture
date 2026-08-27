package com.gardensmc.gardensfurniture.loader;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.item.PlaceOptions;
import com.gardensmc.gardensfurniture.store.FurnitureCache;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import com.gardensmc.gardensfurniture.structure.StructureHandler;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;

import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

public class ChunkFurnitureLoader {

    public static final Queue<FurnitureReplacement> furnitureReplacementQueue = new ConcurrentLinkedQueue<>();
    public static ChunkFurnitureLoader INSTANCE = new ChunkFurnitureLoader();
    private static final int BLOCKS_PER_TICK = 10000;

    public void processSnapshot(ChunkSnapshot chunkSnapshot, World.Environment environment) {
        var yMin = environment == World.Environment.NORMAL ? -60 : 0;
        for (int x = 0; x < 16; x++) {
            for (int y = 230; y > yMin; y--) {
                for (int z = 0; z < 16; z++) {
                    BlockData blockData = chunkSnapshot.getBlockData(x, y, z);
                    if (blockData instanceof NoteBlock noteBlock) {
                        String key = String.format(
                                "[%s, %s, %s]",
                                noteBlock.getNote().getId(),
                                noteBlock.getInstrument().toString().toLowerCase(),
                                noteBlock.isPowered()

                        );
                        int finalX = x;
                        int finalY = y;
                        int finalZ = z;
                        String mapping = ConfigUtil.generationMappings.getString(key);
                        if (mapping != null) {
                            furnitureReplacementQueue.add(new FurnitureReplacement(chunkSnapshot.getWorldName(), chunkSnapshot.getX(), chunkSnapshot.getZ(), finalX, finalY, finalZ, mapping));
                        } else {
                            GardensFurniture.plugin.getLogger().severe("Could not find mapping with key: " + key);
                        }
                    }
                }
            }
        }

        // limit cache update to once per chunk
//        if (updateCache) {
//            Bukkit.getScheduler().runTask(GardensFurniture.plugin, () -> FurnitureCache.INSTANCE.refreshChunk(new FurnitureCache.ChunkKey(chunkSnapshot.getWorldName(), chunkSnapshot.getX(),chunkSnapshot.getZ())));
//        }
    }

    public void tickChunkFurnitureLoader() {
        tickChunkFurnitureLoader(BLOCKS_PER_TICK);
    }

    public void loadAllQueuedFurniture() {
        tickChunkFurnitureLoader(Integer.MAX_VALUE);
    }

    public void tickChunkFurnitureLoader(int bps) {
        var affectedChunks = new HashSet<Chunk>();
        for (int i = 0; i < bps; i++) {
            var furnitureReplacement = furnitureReplacementQueue.poll();
            if (furnitureReplacement == null) {
                break;
            }
            affectedChunks.add(placeFurnitureBlock(furnitureReplacement));
        }
        affectedChunks.forEach(chunk -> FurnitureCache.INSTANCE.refreshChunk(new FurnitureCache.ChunkKey(chunk)));
    }

    public void tickChunkStats() {
        if (!furnitureReplacementQueue.isEmpty()) {
            System.out.println("Replacing furniture with %s blocks in queue...".formatted(furnitureReplacementQueue.size()));
        }
    }

    public Chunk placeFurnitureBlock(FurnitureReplacement furnitureReplacement) {
        var bukkitWorld = Bukkit.getWorld(furnitureReplacement.world);
        var chunk = Objects.requireNonNull(bukkitWorld).getChunkAt(furnitureReplacement.chunkX, furnitureReplacement.chunkZ);
        // safety in-case chunk unloaded by the time we process it synchronously
        if (!chunk.isLoaded()) {
            chunk.load();
        }
        Block block = chunk.getBlock(furnitureReplacement.relX, furnitureReplacement.relY, furnitureReplacement.relZ);
        // custom mappings
        var mapping = furnitureReplacement.mapping;
        CustomItem customItem = GardensFurniture.itemRegistry.getItem(mapping);
        block.setType(Material.AIR);
        // World-gen furniture is deterministic and regenerable; never record it as player activity.
        FurnitureStoreHandler.suppressLifecycle.set(true);
        try {
            if (customItem instanceof CustomPlaceableItem placeableItem) {
                placeableItem.placeBlock(null, null, block.getLocation(), new PlaceOptions().setPersistentLeaves(false), null);
            } else {
                if (!StructureHandler.INSTANCE.spawnStructure(mapping, block.getLocation())) {
                    GardensFurniture.plugin.getLogger().log(Level.SEVERE, "Generation mapping " + mapping + " is not a placeable custom block or structure!");
                }
            }
        } finally {
            FurnitureStoreHandler.suppressLifecycle.set(false);
        }
        return chunk;
    }

    public record FurnitureReplacement(String world, int chunkX, int chunkZ, int relX, int relY, int relZ, String mapping) {}
}
