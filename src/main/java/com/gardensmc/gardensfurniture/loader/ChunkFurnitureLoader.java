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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    /**
     * Note block states vanilla generation places itself (a chime note block shows up in vanilla structures), so they
     * aren't world-gen markers: left as generated, never logged, and refused as keys in generation_mappings.yml.
     */
    static final Set<String> VANILLA_KEYS = Set.of("[0, chime, false]");
    private static final int BLOCKS_PER_TICK = 10000;
    // stop placing once this much of the tick is used, however many blocks are left
    private static final long TICK_BUDGET_NANOS = 5_000_000;
    private final Map<String, List<FurnitureReplacement>> deferredByChunk = new ConcurrentHashMap<>();
    // chunk scans started but not finished, by world: their blocks aren't queued yet
    private final Map<String, java.util.concurrent.atomic.AtomicInteger> scansInFlight = new ConcurrentHashMap<>();

    /** A new chunk's scan is about to run off the main thread (see {@link #processSnapshot}). */
    public void scanStarted(String world) {
        scansInFlight.computeIfAbsent(world, w -> new java.util.concurrent.atomic.AtomicInteger()).incrementAndGet();
    }

    public void scanFinished(String world) {
        var count = scansInFlight.get(world);
        if (count != null) count.decrementAndGet();
    }

    /**
     * How much world-gen furniture is still to be placed in a world: chunks still being scanned, blocks queued, and
     * blocks waiting for their chunk to load. Zero once every generated chunk so far has its furniture. Other plugins
     * wait for this before unloading a world they've pregenerated (GardensPlayers' resource worlds), since anything
     * left for an unloaded world is dropped.
     */
    public int pending(String world) {
        var scans = scansInFlight.get(world);
        int count = scans == null ? 0 : scans.get();
        for (var replacement : furnitureReplacementQueue) {
            if (replacement.world().equals(world)) count++;
        }
        var prefix = world + ",";
        for (var entry : deferredByChunk.entrySet()) {
            if (entry.getKey().startsWith(prefix)) count += entry.getValue().size();
        }
        return count;
    }

    // generated note blocks can sit anywhere in the world's height (e.g. icicles on peaks above y=230). Air-only
    // sections are skipped, and a block's type is checked before building its BlockData, which most blocks never need
    public void processSnapshot(ChunkSnapshot chunkSnapshot, int minY, int maxY) {
        for (int sectionY = maxY - 16; sectionY >= minY; sectionY -= 16) {
            if (chunkSnapshot.isSectionEmpty((sectionY - minY) >> 4)) continue;
            for (int y = sectionY + 15; y >= sectionY; y--) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        if (chunkSnapshot.getBlockType(x, y, z) != Material.NOTE_BLOCK) continue;
                        processNoteBlock(chunkSnapshot, x, y, z);
                    }
                }
            }
        }
    }

    private void processNoteBlock(ChunkSnapshot chunkSnapshot, int x, int y, int z) {
        if (!(chunkSnapshot.getBlockData(x, y, z) instanceof NoteBlock noteBlock)) return;
        String key = "[" + noteBlock.getNote().getId() + ", " + noteBlock.getInstrument().toString().toLowerCase() + ", " + noteBlock.isPowered() + "]";
        if (VANILLA_KEYS.contains(key)) return;
        String mapping = ConfigUtil.generationMappings.getString(key);
        if (mapping != null) {
            furnitureReplacementQueue.add(new FurnitureReplacement(chunkSnapshot.getWorldName(), chunkSnapshot.getX(), chunkSnapshot.getZ(), x, y, z, mapping));
        } else if (reportedMissingMappings.add(key)) {
            GardensFurniture.plugin.getLogger().severe("Could not find mapping with key: " + key);
        }
    }

    // report mappings that point at nothing once at startup instead of once per generated block
    public void validateMappings() {
        for (String key : ConfigUtil.generationMappings.getKeys(false)) {
            if (VANILLA_KEYS.contains(key)) {
                GardensFurniture.plugin.getLogger().severe("generation_mappings.yml: " + key + " is a note block vanilla generates itself, so it's ignored"
                        + " - pick another note or instrument for " + ConfigUtil.generationMappings.getString(key));
                continue;
            }
            var mapping = ConfigUtil.generationMappings.getString(key);
            if (mapping != null && !(GardensFurniture.itemRegistry.getItem(mapping) instanceof CustomPlaceableItem)
                    && !StructureHandler.INSTANCE.hasStructure(mapping) && reportedMissingMappings.add(mapping)) {
                GardensFurniture.plugin.getLogger().severe("generation_mappings.yml: " + key + " -> " + mapping + " is not a placeable custom block or structure");
            }
        }
    }

    public void tickChunkFurnitureLoader() {
        tickChunkFurnitureLoader(BLOCKS_PER_TICK, TICK_BUDGET_NANOS, false);
    }

    // admin command: drain everything now, loading chunks as needed
    public void loadAllQueuedFurniture() {
        tickChunkFurnitureLoader(Integer.MAX_VALUE, Long.MAX_VALUE, true);
    }

    private void tickChunkFurnitureLoader(int bps, long budgetNanos, boolean loadChunksSync) {
        var affectedChunks = new HashSet<Chunk>();
        long start = System.nanoTime();
        for (int i = 0; i < bps && System.nanoTime() - start < budgetNanos; i++) {
            var furnitureReplacement = furnitureReplacementQueue.poll();
            if (furnitureReplacement == null) {
                break;
            }
            var world = Bukkit.getWorld(furnitureReplacement.world);
            if (world == null) {
                continue;
            }
            if (!loadChunksSync && !world.isChunkLoaded(furnitureReplacement.chunkX, furnitureReplacement.chunkZ)) {
                deferUntilLoaded(world, furnitureReplacement);
                continue;
            }
            affectedChunks.add(placeFurnitureBlock(furnitureReplacement));
        }
        affectedChunks.forEach(chunk -> FurnitureCache.INSTANCE.refreshChunk(new FurnitureCache.ChunkKey(chunk)));
    }

    // The chunk unloaded before we got to it: load it in the background, instead of synchronously on the main thread,
    // and place its blocks as soon as it's in. Placed right there, not queued again: a chunk loaded this way holds
    // no ticket, so it could unload again before the queue came round, and the blocks would go round forever.
    private void deferUntilLoaded(World world, FurnitureReplacement replacement) {
        var key = world.getName() + "," + replacement.chunkX + "," + replacement.chunkZ;
        var waiting = deferredByChunk.computeIfAbsent(key, k -> new ArrayList<>());
        waiting.add(replacement);
        if (waiting.size() == 1) {
            world.getChunkAtAsync(replacement.chunkX, replacement.chunkZ).whenComplete((chunk, error) -> {
                var ready = deferredByChunk.remove(key);
                if (ready == null) return;
                if (error != null || chunk == null) {
                    GardensFurniture.plugin.getLogger().log(Level.WARNING, "Couldn't load chunk " + key + " to place " + ready.size() + " world-gen furniture blocks", error);
                    return;
                }
                ready.forEach(this::placeFurnitureBlock);
                FurnitureCache.INSTANCE.refreshChunk(new FurnitureCache.ChunkKey(chunk));
            });
        }
    }

    private int lastReported;

    // a line when the backlog changes, not every 5 seconds while it sits there
    public void tickChunkStats() {
        int queued = furnitureReplacementQueue.size();
        if (queued != lastReported && (queued > 0 || lastReported > 0)) {
            GardensFurniture.plugin.getLogger().info(queued == 0 ? "World-gen furniture placed" : "Placing world-gen furniture: " + queued + " blocks queued");
        }
        lastReported = queued;
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
