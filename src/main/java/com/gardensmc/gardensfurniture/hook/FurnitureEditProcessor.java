package com.gardensmc.gardensfurniture.hook;

import com.fastasyncworldedit.core.extent.processor.IBatchProcessorHolder;
import com.fastasyncworldedit.core.extent.processor.MultiBatchProcessor;
import com.fastasyncworldedit.core.history.changeset.AbstractChangeSet;
import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.gardensmc.gardensfurniture.store.FurnitureCache;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs on each chunk a FAWE edit writes, just before FAWE applies it: turns furniture proxies back into
 * their real block and writes their stores, drops the stores of stored blocks the edit overwrites, saves
 * both in the edit's history, and puts back stores an undo or redo restores. One per edit. See {@link WorldEditHook}.
 */
final class FurnitureEditProcessor implements IBatchProcessor {

    private final String world;
    private final IQueueExtent<?> queue;
    // stores an undo handed back, by chunk, waiting for the chunk write that restores their block
    private final Map<Long, Map<FurnitureCache.BlockKey, FurnitureStore>> restores = new ConcurrentHashMap<>();
    // found on first use: the edit's history is set up after this processor is added
    private volatile AbstractChangeSet history;
    private volatile boolean historyLooked;

    FurnitureEditProcessor(String world, IQueueExtent<?> queue) {
        this.world = world;
        this.queue = queue;
    }

    /** An undo restoring a stored block's store: written when the chunk write restoring the block runs. */
    void restore(int x, int y, int z, FurnitureStore store) {
        restores.computeIfAbsent(chunkKey(x >> 4, z >> 4), key -> new ConcurrentHashMap<>())
                .put(new FurnitureCache.BlockKey(x, y, z), store);
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return (long) chunkX << 32 | (chunkZ & 0xFFFFFFFFL);
    }

    private record Proxy(int x, int y, int z, FaweCompoundTag tag) {}

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        int baseX = chunk.getX() << 4, baseZ = chunk.getZ() << 4;
        var changes = new ArrayList<WorldEditHook.StoreChange>();
        var placed = new HashSet<FurnitureCache.BlockKey>();

        var tiles = set.tiles();
        if (!tiles.isEmpty()) {
            // copied out first: the entry view reuses one mutable entry, and the map is changed below
            var proxies = new ArrayList<Proxy>();
            for (var entry : tiles.entrySet()) {
                var key = entry.getKey();
                if (entry.getValue().linTag().value().containsKey(WorldEditHook.PROXY_KEY)) {
                    proxies.add(new Proxy(key.x() & 15, key.y(), key.z() & 15, entry.getValue()));
                }
            }
            for (var proxy : proxies) {
                var furniture = WorldEditHook.fromProxy(set.getBlock(proxy.x(), proxy.y(), proxy.z()), proxy.tag().linTag());
                if (furniture == null) continue;
                set.setBlock(proxy.x(), proxy.y(), proxy.z(), furniture.state());
                tiles.remove(BlockVector3.at(proxy.x(), proxy.y(), proxy.z()));
                int x = baseX + proxy.x(), z = baseZ + proxy.z();
                changes.add(new WorldEditHook.StoreChange(x, proxy.y(), z, furniture.store()));
                var key = new FurnitureCache.BlockKey(x, proxy.y(), z);
                placed.add(key);
                remember(key, furniture.store(), true);
            }
        }

        var pending = restores.get(chunkKey(chunk.getX(), chunk.getZ()));
        if (pending != null) {
            for (var entry : pending.entrySet()) {
                var key = entry.getKey();
                if (!placed.contains(key) && WorldEditHook.isSet(set, key.x(), key.y(), key.z())
                        && pending.remove(key, entry.getValue())) {
                    changes.add(new WorldEditHook.StoreChange(key.x(), key.y(), key.z(), entry.getValue()));
                    placed.add(key);
                }
            }
        }

        WorldEditHook.SetBlocks uncached = null;
        var stores = FurnitureCache.INSTANCE.getCachedChunkFurniture(new FurnitureCache.ChunkKey(world, chunk.getX(), chunk.getZ()));
        if (stores == null) {
            uncached = WorldEditHook.SetBlocks.copyOf(chunk.getX(), chunk.getZ(), set);
        } else {
            for (var entry : stores.entrySet()) {
                var key = entry.getKey();
                if (!placed.contains(key) && WorldEditHook.isSet(set, key.x(), key.y(), key.z())) {
                    changes.add(new WorldEditHook.StoreChange(key.x(), key.y(), key.z(), null));
                    remember(key, entry.getValue(), false);
                }
            }
        }

        if (!changes.isEmpty() || uncached != null) {
            WorldEditHook.apply(world, changes, uncached);
        }
        return set;
    }

    /**
     * Saves a store in the edit's history as a block entity the edit created (a placed proxy) or removed (an
     * overwritten block), which a redo or an undo hands back to {@link GardensWorldEditExtent#tile} - the
     * history only keeps the block itself otherwise, and proxies are gone before it records the chunk.
     */
    private void remember(FurnitureCache.BlockKey key, FurnitureStore store, boolean created) {
        var history = history();
        if (history == null) return;
        var tag = FaweCompoundTag.of(WorldEditHook.toRestoreTag(key.x(), key.y(), key.z(), store));
        // the history's own processSet holds this lock while it writes the same stream
        synchronized (history) {
            if (created) {
                history.addTileCreate(tag);
            } else {
                history.addTileRemove(tag);
            }
        }
    }

    @Nullable
    private AbstractChangeSet history() {
        if (!historyLooked) {
            history = findHistory(queue);
            historyLooked = true;
        }
        return history;
    }

    // FAWE's ProcessorTraverser misses it: with history.send-before-history it sits in a holder's
    // post-processor, which the traverser never looks at
    @Nullable
    private static AbstractChangeSet findHistory(IBatchProcessorHolder root) {
        var seen = Collections.newSetFromMap(new IdentityHashMap<IBatchProcessor, Boolean>());
        var pending = new ArrayDeque<IBatchProcessor>();
        pending.add(root);
        IBatchProcessor processor;
        while ((processor = pending.poll()) != null) {
            if (!seen.add(processor)) continue;
            if (processor instanceof AbstractChangeSet history) {
                return history;
            }
            if (processor instanceof MultiBatchProcessor multi) {
                pending.addAll(multi.getBatchProcessors());
            } else if (processor instanceof IBatchProcessorHolder holder) {
                pending.add(holder.getProcessor());
                pending.add(holder.getPostProcessor());
            }
        }
        return null;
    }

    @Override
    public Extent construct(Extent child) {
        // only ever added to FAWE queues, which run processors themselves
        return child;
    }
}
