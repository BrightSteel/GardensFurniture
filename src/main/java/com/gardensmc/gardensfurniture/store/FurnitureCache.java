package com.gardensmc.gardensfurniture.store;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import de.tr7zw.nbtapi.NBTChunk;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static com.gardensmc.gardensfurniture.store.FurnitureStoreHandler.BLOCK_DATA_KEY;
import static com.gardensmc.gardensfurniture.store.FurnitureStoreHandler.IDENTIFIER_KEY;

public class FurnitureCache {

    public static FurnitureCache INSTANCE = new FurnitureCache();

    private final LoadingCache<ChunkKey, Map<BlockKey, FurnitureStore>> furnitureCache;

    public FurnitureCache() {
        // Entries live exactly as long as their chunk is loaded: filled on ChunkLoadEvent (main thread),
        // dropped on ChunkUnloadEvent. Packet threads read chunks while they're sent, so a time-based expiry
        // would force them to load chunk data off the main thread.
        furnitureCache = CacheBuilder.newBuilder()
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull Map<BlockKey, FurnitureStore> load(@NotNull ChunkKey key) {
                        var world = Bukkit.getWorld(key.world);
                        if (world == null) return new ConcurrentHashMap<>();
                        var chunk = world.getChunkAt(key.x, key.z);
                        return loadChunkFurniture(chunk);
                    }
                });
    }

    public Map<BlockKey, FurnitureStore> getChunkFurniture(Chunk chunk) {
        try {
            return furnitureCache.get(new ChunkKey(chunk));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public FurnitureStore getFurnitureStore(ChunkKey chunkKey, BlockKey blockKey) {
        Map<BlockKey, FurnitureStore> chunkData;
        if (!Bukkit.isPrimaryThread()) {
            // off the main thread (packet listeners) never load a chunk or read its PDC - loaded chunks
            // are always cached, so a miss means there's no furniture data to show
            chunkData = furnitureCache.getIfPresent(chunkKey);
            if (chunkData == null) return null;
        } else {
            try {
                chunkData = furnitureCache.get(chunkKey);
            } catch (ExecutionException e) {
                e.printStackTrace();
                return null;
            }
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
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(GardensFurniture.plugin, () -> furnitureCache.refresh(chunkKey));
            return;
        }
        furnitureCache.refresh(chunkKey);
    }

    // the stores of a chunk if it's cached, without ever loading it (safe from any thread)
    @Nullable
    public Map<BlockKey, FurnitureStore> getCachedChunkFurniture(ChunkKey chunkKey) {
        return furnitureCache.getIfPresent(chunkKey);
    }

    // keep a cached chunk in step with a single store write, without re-reading the whole chunk PDC
    public void putCached(ChunkKey chunkKey, BlockKey blockKey, FurnitureStore store) {
        var chunkData = furnitureCache.getIfPresent(chunkKey);
        if (chunkData != null) {
            chunkData.put(blockKey, store);
        }
    }

    public void removeCached(ChunkKey chunkKey, BlockKey blockKey) {
        var chunkData = furnitureCache.getIfPresent(chunkKey);
        if (chunkData != null) {
            chunkData.remove(blockKey);
        }
    }

    public void invalidateChunk(ChunkKey chunkKey) {
        furnitureCache.invalidate(chunkKey);
    }

    // chunks loaded before the plugin enabled (spawn chunks) never fire a ChunkLoadEvent for us
    public void loadAllLoadedChunks() {
        for (var world : Bukkit.getWorlds()) {
            for (var chunk : world.getLoadedChunks()) {
                loadChunk(new ChunkKey(chunk));
            }
        }
    }

    public record ChunkKey(String world, int x, int z) {
        public ChunkKey(Chunk chunk) {
            this(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        }
    }

    public record BlockKey(int x, int y, int z) {}

    private Map<BlockKey, FurnitureStore> loadChunkFurniture(Chunk chunk) {
        // buffered world-gen stores go into the chunk's data first, or reading it would lose them
        if (Bukkit.isPrimaryThread()) FurnitureStoreHandler.INSTANCE.flush(chunk);
        var nbt = new NBTChunk(chunk).getPersistentDataContainer();
        var compound = nbt.getCompound("GardensFurniture");
        // concurrent + mutable: written in place on the main thread while packet threads read it
        var blockMap = new ConcurrentHashMap<BlockKey, FurnitureStore>();
        if (compound == null) return blockMap;
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
