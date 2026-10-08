package com.gardensmc.gardensfurniture.store;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;
import de.tr7zw.nbtapi.NBTContainer;
import com.gardensmc.gardensfurniture.GardensFurniture;
import de.tr7zw.nbtapi.NBTChunk;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class FurnitureStoreHandler {

    public static FurnitureStoreHandler INSTANCE = new FurnitureStoreHandler();
    public static final String IDENTIFIER_KEY = "customItem";
    public static final String BLOCK_DATA_KEY = "furnitureStore";

    /**
     * Lifecycle hook for block/hybrid furniture (anything backed by a {@link FurnitureStore}).
     * Invoked with {@code (block, true)} right after a store is written and {@code (block, false)}
     * right before a store is removed — the canonical "furniture appeared / is about to disappear"
     * signal. Kept as a plain {@link BiConsumer} so this core class carries no dependency on any
     * optional integration (e.g. Prism); the hook is only registered when that plugin is present.
     */
    @Nullable
    public static volatile BiConsumer<Block, Boolean> lifecycleListener;

    /**
     * When true on the current thread, lifecycle notifications are suppressed. Used to silence
     * world-gen furniture placement and our own rollback/restore rebuilds, which must never be
     * re-recorded as new player activity.
     */
    public static final ThreadLocal<Boolean> suppressLifecycle = ThreadLocal.withInitial(() -> false);

    /**
     * Best-effort "who is causing the current furniture change" for the current thread (a Player,
     * or null). Set at the player-facing place/break entry points so lifecycle recording can attribute
     * a cause. Null falls back to a system cause.
     */
    public static final ThreadLocal<Object> currentActor = new ThreadLocal<>();

    /**
     * Block state of a store's block from before it was broken, for removals that run after the block
     * is already gone (so lifecycle recording captures the real block rather than air).
     */
    public static final ThreadLocal<String> removedBlockData = new ThreadLocal<>();

    /**
     * World-gen placement buffers its stores here, per chunk, instead of writing each to the chunk's data as it's
     * placed: every write through NBT-API re-encodes the chunk's whole store, so a chunk of 140 End blocks cost ~140²
     * of them. {@link #flush} writes a chunk's buffered stores in one go; it runs when the chunk's last block is
     * placed, when the chunk unloads, and before anything reads the chunk's stores. Main thread only.
     */
    public static boolean buffering;
    private final Map<FurnitureCache.ChunkKey, Map<String, FurnitureStore>> buffered = new HashMap<>();

    public void addFurnitureStore(Block block, FurnitureStore furnitureStore, boolean refresh) {
        if (buffering && !refresh) {
            var chunkKey = new FurnitureCache.ChunkKey(block.getChunk());
            buffered.computeIfAbsent(chunkKey, k -> new LinkedHashMap<>()).put(getBlockKey(block), furnitureStore);
            FurnitureCache.INSTANCE.putCached(chunkKey, blockKey(block), furnitureStore);
            notifyLifecycle(block, true);
            return;
        }
        var nbtChunk = new NBTChunk(block.getChunk()).getPersistentDataContainer().getOrCreateCompound("GardensFurniture");
        var chunkBlockCompound = nbtChunk.getOrCreateCompound(getBlockKey(block));

        chunkBlockCompound.setString(IDENTIFIER_KEY, furnitureStore.getItemIdentifier());
        var blockData = chunkBlockCompound.addCompound(BLOCK_DATA_KEY);
        if (furnitureStore.getFacing() != null) {
            blockData.setString("facing", furnitureStore.getFacing());
        }
        if (furnitureStore.getMultiplier() != 0) {
            blockData.setInteger("multiplier", furnitureStore.getMultiplier());
        }
        if (refresh) {
            FurnitureCache.INSTANCE.refreshChunk(new FurnitureCache.ChunkKey(block.getWorld().getName(), block.getChunk().getX(), block.getChunk().getZ()));
        } else {
            FurnitureCache.INSTANCE.putCached(new FurnitureCache.ChunkKey(block.getChunk()), blockKey(block), furnitureStore);
        }
        notifyLifecycle(block, true);
    }

    /** Writes a chunk's buffered stores (see {@link #buffering}) to its data, in one write. */
    public void flush(Chunk chunk) {
        var stores = buffered.remove(new FurnitureCache.ChunkKey(chunk));
        if (stores == null || stores.isEmpty()) return;
        var batch = new NBTContainer();
        stores.forEach((key, store) -> {
            var compound = batch.addCompound(key);
            compound.setString(IDENTIFIER_KEY, store.getItemIdentifier());
            var blockData = compound.addCompound(BLOCK_DATA_KEY);
            if (store.getFacing() != null) blockData.setString("facing", store.getFacing());
            if (store.getMultiplier() != 0) blockData.setInteger("multiplier", store.getMultiplier());
        });
        new NBTChunk(chunk).getPersistentDataContainer().getOrCreateCompound("GardensFurniture").mergeCompound(batch);
    }

    /** Flushes every buffered chunk of a world (all worlds for null): at world saves and shutdown. */
    public void flushAll(org.bukkit.World only) {
        for (var key : java.util.List.copyOf(buffered.keySet())) {
            var world = org.bukkit.Bukkit.getWorld(key.world());
            if (world == null || (only != null && !only.equals(world))) continue;
            flush(world.getChunkAt(key.x(), key.z()));
        }
    }

    public boolean hasBuffered(FurnitureCache.ChunkKey chunkKey) {
        return buffered.containsKey(chunkKey);
    }

    public void removeFurnitureStore(Block block) {
        // a store still buffered for this chunk goes first, so it can't be written back after its removal
        flush(block.getChunk());
        // Notify before the store is gone so listeners can still read the furniture spec.
        notifyLifecycle(block, false);
        var nbtChunk = new NBTChunk(block.getChunk()).getPersistentDataContainer().getOrCreateCompound("GardensFurniture");
        nbtChunk.removeKey(getBlockKey(block));
        FurnitureCache.INSTANCE.removeCached(new FurnitureCache.ChunkKey(block.getChunk()), blockKey(block));
        FurnitureCache.INSTANCE.refreshChunk(new FurnitureCache.ChunkKey(block.getWorld().getName(), block.getChunk().getX(), block.getChunk().getZ()));
    }

    /**
     * For stores written ahead of a vanilla ("natural") placement: if vanilla didn't end up placing the
     * block (placement failed, or a slab merged into the clicked block instead), drop the store next tick
     * so it can't disguise whatever vanilla block is placed there later.
     */
    public void removeIfNotPlaced(Block block, Predicate<Material> isPlaced) {
        Bukkit.getScheduler().runTaskLater(GardensFurniture.plugin, () -> {
            if (isPlaced.test(block.getType())) {
                return;
            }
            // the placement never happened, so don't record it as a break either
            suppressLifecycle.set(true);
            try {
                removeFurnitureStore(block);
            } finally {
                suppressLifecycle.set(false);
            }
        }, 1);
    }

    private void notifyLifecycle(Block block, boolean added) {
        var listener = lifecycleListener;
        if (listener == null || Boolean.TRUE.equals(suppressLifecycle.get())) {
            return;
        }
        if (added) {
            // Ensure the cache reflects the just-written store so the listener can read its spec,
            // even when the caller passed refresh=false.
            FurnitureCache.INSTANCE.refreshChunk(new FurnitureCache.ChunkKey(block.getChunk()));
        }
        listener.accept(block, added);
    }

    @Nullable
    public FurnitureStore getFurnitureStore(Block block) {
        return getFurnitureStore(block.getChunk(), block.getX(), block.getY(), block.getZ());
    }

    @Nullable
    public FurnitureStore getFurnitureStore(Chunk chunk, int x, int y, int z) {
        return FurnitureCache.INSTANCE.getFurnitureStore(
                new FurnitureCache.ChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()),
                new FurnitureCache.BlockKey(x, y, z)
        );
    }

    /** Every block in this chunk with a store, read from the chunk's own data (for the cleanup scan). */
    public java.util.List<Block> storedBlocks(Chunk chunk) {
        var stores = new NBTChunk(chunk).getPersistentDataContainer().getCompound("GardensFurniture");
        if (stores == null) return java.util.List.of();
        var blocks = new java.util.ArrayList<Block>();
        for (String key : stores.getKeys()) {
            var parts = key.split("\\.");
            if (parts.length != 3) continue;
            try {
                blocks.add(chunk.getWorld().getBlockAt(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
            } catch (NumberFormatException ignored) {
                // not one of ours
            }
        }
        return blocks;
    }

    private FurnitureCache.BlockKey blockKey(Block block) {
        return new FurnitureCache.BlockKey(block.getX(), block.getY(), block.getZ());
    }

    private String getBlockKey(Block block) {
        return getBlockKey(block.getX(), block.getY(), block.getZ());
    }

    private String getBlockKey(int x, int y, int z) {
        return String.format("%s.%s.%s", x, y, z);
    }
}
