package com.gardensmc.gardensfurniture.store;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import de.tr7zw.nbtapi.NBTChunk;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.gardensmc.gardensfurniture.store.FurnitureStoreHandler.BLOCK_DATA_KEY;
import static com.gardensmc.gardensfurniture.store.FurnitureStoreHandler.IDENTIFIER_KEY;

public class FurnitureCache {

    public static FurnitureCache INSTANCE = new FurnitureCache();

    private final LoadingCache<ChunkKey, Map<BlockKey, FurnitureStore>> furnitureCache;

    public FurnitureCache() {
        furnitureCache = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull Map<BlockKey, FurnitureStore> load(@NotNull ChunkKey key) {
                        var world = Bukkit.getWorld(key.world);
                        if (world == null) return Collections.emptyMap();
                        var chunk = world.getChunkAt(key.x, key.z);
                        return loadChunkFurniture(chunk);
                    }
                });
    }

    public FurnitureStore  getFurnitureStore(ChunkKey chunkKey, BlockKey blockKey) {
        Map<BlockKey, FurnitureStore> chunkData;
        try {
            chunkData = furnitureCache.get(chunkKey);
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }

        if (chunkData.isEmpty()) return null;
        return chunkData.get(blockKey);
    }

    public void loadChunk(ChunkKey chunkKey) {
        try {
            furnitureCache.get(chunkKey);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void refreshChunk(ChunkKey chunkKey) {
        furnitureCache.refresh(chunkKey);
    }

    public record ChunkKey(String world, int x, int z) {
        public ChunkKey(Chunk chunk) {
            this(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        }
    }

    public record BlockKey(int x, int y, int z) {}

    private Map<BlockKey, FurnitureStore> loadChunkFurniture(Chunk chunk) {
        var nbt = new NBTChunk(chunk).getPersistentDataContainer();
        var compound = nbt.getCompound("GardensFurniture");
        if (compound == null) return Collections.emptyMap();
        var blockMap = new HashMap<BlockKey, FurnitureStore>();
        for (String key : compound.getKeys()) {
            String[] coords = key.split("\\.");
            BlockKey blockKey = new BlockKey(
                    Integer.parseInt(coords[0]),
                    Integer.parseInt(coords[1]),
                    Integer.parseInt(coords[2])
            );
            var chunkBlockCompound = compound.getCompound(key);
            var customItem = chunkBlockCompound.getString(IDENTIFIER_KEY);
            var blockData = chunkBlockCompound.getCompound(BLOCK_DATA_KEY);
            // no blockData
            FurnitureStore furnitureStore;
            if (blockData == null) {
                furnitureStore =  new FurnitureStore(customItem);
            } else {
                int multiplier = blockData.hasTag("multiplier") ? blockData.getInteger("multiplier") : 0;
                furnitureStore = new FurnitureStore(
                        customItem,
                        multiplier,
                        blockData.getString("facing")
                );
            }
            blockMap.put(blockKey, furnitureStore);
        }
        return blockMap;
    }
}
