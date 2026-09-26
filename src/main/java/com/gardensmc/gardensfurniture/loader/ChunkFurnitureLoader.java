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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

public class ChunkFurnitureLoader {

    public static final Queue<FurnitureReplacement> furnitureReplacementQueue = new ConcurrentLinkedQueue<>();
    public static ChunkFurnitureLoader INSTANCE = new ChunkFurnitureLoader();
    private final Set<String> reportedMissingMappings = ConcurrentHashMap.newKeySet();
    private static final int BLOCKS_PER_TICK = 10000;

    // generated note blocks can sit anywhere in the world's height (e.g. icicles on peaks above y=230)
    public void processSnapshot(ChunkSnapshot chunkSnapshot, int minY, int maxY) {
        for (int x = 0; x < 16; x++) {
            for (int y = maxY - 1; y >= minY; y--) {
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
                        } else if (reportedMissingMappings.add(key)) {
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

    // report mappings that point at nothing once at startup instead of once per generated block
    public void validateMappings() {
        for (String key : ConfigUtil.generationMappings.getKeys(false)) {
            var mapping = ConfigUtil.generationMappings.getString(key);
            if (mapping != null && !(GardensFurniture.itemRegistry.getItem(mapping) instanceof CustomPlaceableItem)
                    && !StructureHandler.INSTANCE.hasStructure(mapping) && reportedMissingMappings.add(mapping)) {
                GardensFurniture.plugin.getLogger().severe("generation_mappings.yml: " + key + " -> " + mapping + " is not a placeable custom block or structure");
            }
        }
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
        // the block may have changed since the snapshot was taken
        if (block.getType() != Material.NOTE_BLOCK) {
            return chunk;
        }
        CustomItem customItem = GardensFurniture.itemRegistry.getItem(mapping);
        if (!(customItem instanceof CustomPlaceableItem) && !StructureHandler.INSTANCE.hasStructure(mapping)) {
            // leave the note block rather than punching a hole in the terrain
            if (reportedMissingMappings.add(mapping)) {
                GardensFurniture.plugin.getLogger().severe("Generation mapping " + mapping + " is not a placeable custom block or structure!");
            }
            return chunk;
        }
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
