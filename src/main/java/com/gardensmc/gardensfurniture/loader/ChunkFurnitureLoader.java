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
    private static final int BLOCKS_PER_TICK = 1_000_000;
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
        for (var entry : byChunk.entrySet()) {
            if (entry.getKey().world().equals(world)) count += entry.getValue().size();
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

    /**
     * Places queued world-gen furniture for a slice of each tick (config.yml {@code world_gen_furniture}): a little on
     * a live server, much more with nobody online, so a server pregenerating worlds keeps up with Chunky.
     */
    public void tickChunkFurnitureLoader() {
        intake();
        if (order.isEmpty()) return;
        var config = GardensFurniture.plugin.getConfig();
        double millis = Bukkit.getOnlinePlayers().isEmpty()
                ? config.getDouble("world_gen_furniture.tick_budget_ms_empty", 20)
                : config.getDouble("world_gen_furniture.tick_budget_ms", 5);
        placeRound((long) (Math.max(0.5, Math.min(30, millis)) * 1_000_000));
    }

    // admin command: drain everything now, loading chunks as needed
    public void loadAllQueuedFurniture() {
        intake();
        var affected = new HashSet<Chunk>();
        for (var entry : byChunk.entrySet()) {
            for (var replacement : entry.getValue()) {
                if (Bukkit.getWorld(replacement.world) != null) affected.add(placeFurnitureBlock(replacement));
            }
        }
        byChunk.keySet().forEach(this::release);
        byChunk.clear();
        order.clear();
        affected.forEach(chunk -> FurnitureCache.INSTANCE.refreshChunk(new FurnitureCache.ChunkKey(chunk)));
    }

    // Blocks waiting per chunk (main thread only), and the chunks in the order they're served. The scans' queue
    // (furnitureReplacementQueue) is moved in here each tick.
    private final Map<FurnitureCache.ChunkKey, java.util.ArrayDeque<FurnitureReplacement>> byChunk = new java.util.LinkedHashMap<>();
    private final java.util.ArrayDeque<FurnitureCache.ChunkKey> order = new java.util.ArrayDeque<>();
    // chunks being loaded for us, and chunks we hold loaded (with a plugin ticket) until their blocks are placed
    private final Set<FurnitureCache.ChunkKey> loading = new HashSet<>();
    private final Set<FurnitureCache.ChunkKey> held = new HashSet<>();
    /**
     * At most this many chunks are held loaded for their blocks; past it, a chunk unloads as usual and is loaded again
     * for its turn. (Holding every chunk with blocks waiting kept 40,000 End chunks in memory during pregeneration.)
     */
    private static final int MAX_HELD = 2000;

    private void intake() {
        for (var replacement = furnitureReplacementQueue.poll(); replacement != null; replacement = furnitureReplacementQueue.poll()) {
            var key = new FurnitureCache.ChunkKey(replacement.world, replacement.chunkX, replacement.chunkZ);
            byChunk.computeIfAbsent(key, k -> {
                order.add(k);
                // hold it loaded until its blocks are in, rather than let it unload and load it again for its turn
                var world = Bukkit.getWorld(k.world());
                if (held.size() < MAX_HELD && world != null && world.isChunkLoaded(k.x(), k.z())
                        && world.addPluginChunkTicket(k.x(), k.z(), GardensFurniture.plugin)) {
                    held.add(k);
                }
                return new java.util.ArrayDeque<>();
            }).add(replacement);
        }
    }

    /**
     * One round over the chunks waiting: at most one block per chunk per tick, never two in a row into the same one.
     * Each block placed sets off a light update that Paper works out on another thread while it reads the chunk;
     * writing the chunk's next block at that moment can crash that light update ("Palette index out of bounds"),
     * after which Paper never releases the chunk's load ticket, and every such chunk leaks ~460 chunks of memory.
     * A tick apart, the light update is long done. With many chunks waiting (pregeneration) this still places
     * thousands of blocks a tick.
     */
    private void placeRound(long budgetNanos) {
        long start = System.nanoTime();
        int chunks = order.size();
        for (int i = 0; i < chunks && System.nanoTime() - start < budgetNanos; i++) {
            var key = order.poll();
            var waiting = byChunk.get(key);
            if (waiting == null || waiting.isEmpty()) {
                byChunk.remove(key);
                release(key);
                continue;
            }
            var world = Bukkit.getWorld(key.world());
            if (world == null) {
                byChunk.remove(key);
                held.remove(key);
                loading.remove(key);
                continue;
            }
            if (!world.isChunkLoaded(key.x(), key.z())) {
                load(world, key);
                order.add(key);
                continue;
            }
            // placing keeps the furniture cache in step itself (world gen places without a held item, so the store
            // is put straight into the cached chunk): re-reading the chunk's whole store per block made this crawl
            com.gardensmc.gardensfurniture.store.FurnitureStoreHandler.buffering = true;
            Chunk placed;
            try {
                placed = placeFurnitureBlock(waiting.poll());
            } finally {
                com.gardensmc.gardensfurniture.store.FurnitureStoreHandler.buffering = false;
            }
            if (waiting.isEmpty()) {
                com.gardensmc.gardensfurniture.store.FurnitureStoreHandler.INSTANCE.flush(placed);
                byChunk.remove(key);
                release(key);
            } else {
                order.add(key);
            }
        }
    }

    // The chunk unloaded before its turn: load it in the background (not synchronously on the main thread) and hold
    // it loaded with a plugin ticket until its blocks are placed, so it can't unload again in between.
    private void load(World world, FurnitureCache.ChunkKey key) {
        if (held.contains(key) || !loading.add(key)) return;
        world.getChunkAtAsync(key.x(), key.z()).whenComplete((chunk, error) -> {
            loading.remove(key);
            if (error != null || chunk == null) {
                var dropped = byChunk.remove(key);
                GardensFurniture.plugin.getLogger().log(Level.WARNING, "Couldn't load chunk " + key + " to place "
                        + (dropped == null ? 0 : dropped.size()) + " world-gen furniture blocks", error);
                return;
            }
            if (byChunk.containsKey(key) && held.size() < MAX_HELD && chunk.addPluginChunkTicket(GardensFurniture.plugin)) held.add(key);
        });
    }

    private void release(FurnitureCache.ChunkKey key) {
        if (!held.remove(key)) return;
        var world = Bukkit.getWorld(key.world());
        if (world != null) world.removePluginChunkTicket(key.x(), key.z(), GardensFurniture.plugin);
    }

    private int lastReported;

    // a line when the backlog changes, not every 5 seconds while it sits there
    public void tickChunkStats() {
        int queued = furnitureReplacementQueue.size();
        for (var waiting : byChunk.values()) queued += waiting.size();
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
