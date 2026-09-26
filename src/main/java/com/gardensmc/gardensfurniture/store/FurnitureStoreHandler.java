package com.gardensmc.gardensfurniture.store;

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

    public void addFurnitureStore(Block block, FurnitureStore furnitureStore, boolean refresh) {
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

    public void removeFurnitureStore(Block block) {
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
