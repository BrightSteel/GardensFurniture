package com.gardensmc.gardensfurniture.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.BlockVector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * What pistons, explosions, fire, failed placements and the destroy stick leave behind, found around a
 * point and - only when asked - removed.
 *
 * <p>Barrier and LIGHT blocks are also things admins place on purpose (invisible walls, lighting), and
 * nothing marks which ones furniture made. So this only ever looks near the person running it, reports
 * first, and removes on a second, explicit command. A block counts as ours only when no furniture
 * claims it: a barrier that no display's hitbox covers, a light no custom block or display lights.
 *
 * <p>The block scan runs on chunk snapshots off the main thread; each candidate is checked again on
 * the main thread before it is reported, and once more before it is removed.
 */
public final class FurnitureCleanup {

    public static final int MAX_RADIUS = 128;

    /** Everything found. Displays whose item no longer exists are only reported: the config may be back tomorrow. */
    public record Report(List<Block> stores, List<Block> barriers, List<Block> lights, List<Interaction> hitboxes,
                         List<ItemDisplay> unknownItems) {
        public int removable() {
            return stores.size() + barriers.size() + lights.size() + hitboxes.size();
        }
    }

    private FurnitureCleanup() {
    }

    public static CompletableFuture<Report> scan(Location center, int radius) {
        var world = center.getWorld();
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        int minY = Math.max(world.getMinHeight(), cy - radius), maxY = Math.min(world.getMaxHeight() - 1, cy + radius);

        // main thread: snapshots of the loaded chunks in range, and the stores (chunk data, read here)
        var snapshots = new ArrayList<ChunkSnapshot>();
        var stores = new ArrayList<Block>();
        for (int x = (cx - radius) >> 4; x <= (cx + radius) >> 4; x++) {
            for (int z = (cz - radius) >> 4; z <= (cz + radius) >> 4; z++) {
                if (!world.isChunkLoaded(x, z)) continue;
                var chunk = world.getChunkAt(x, z);
                snapshots.add(chunk.getChunkSnapshot(false, false, false));
                for (Block block : FurnitureStoreHandler.INSTANCE.storedBlocks(chunk)) {
                    if (within(block, cx, cy, cz, radius) && !CustomBlockManager.isStoreBacked(block.getType())) {
                        stores.add(block);
                    }
                }
            }
        }

        var hitboxes = new ArrayList<Interaction>();
        for (Interaction interaction : center.getNearbyEntitiesByType(Interaction.class, radius)) {
            if (CustomBlockManager.getFurnitureEntity(interaction.getLocation(), false) == null) hitboxes.add(interaction);
        }
        var unknownItems = new ArrayList<ItemDisplay>();
        for (ItemDisplay display : center.getNearbyEntitiesByType(ItemDisplay.class, radius)) {
            if (isOrphanedFurniture(display)) unknownItems.add(display);
        }

        return CompletableFuture
                .supplyAsync(() -> {
                    // off the main thread: every barrier and light block in range
                    var barrierSpots = new ArrayList<BlockVector>();
                    var lightSpots = new ArrayList<BlockVector>();
                    for (ChunkSnapshot snapshot : snapshots) {
                        int baseX = snapshot.getX() << 4, baseZ = snapshot.getZ() << 4;
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                int wx = baseX + x, wz = baseZ + z;
                                if (Math.abs(wx - cx) > radius || Math.abs(wz - cz) > radius) continue;
                                for (int y = minY; y <= maxY; y++) {
                                    var type = snapshot.getBlockType(x, y, z);
                                    if (type == Material.BARRIER) barrierSpots.add(new BlockVector(wx, y, wz));
                                    else if (type == Material.LIGHT) lightSpots.add(new BlockVector(wx, y, wz));
                                }
                            }
                        }
                    }
                    return new List[]{barrierSpots, lightSpots};
                })
                .thenApplyAsync(spots -> {
                    // back on the main thread: keep only what no furniture claims
                    @SuppressWarnings("unchecked") List<BlockVector> barrierSpots = spots[0], lightSpots = spots[1];
                    var barriers = new ArrayList<Block>();
                    for (BlockVector spot : barrierSpots) {
                        var block = world.getBlockAt(spot.getBlockX(), spot.getBlockY(), spot.getBlockZ());
                        if (isOrphanedBarrier(block)) barriers.add(block);
                    }
                    var lights = new ArrayList<Block>();
                    for (BlockVector spot : lightSpots) {
                        var block = world.getBlockAt(spot.getBlockX(), spot.getBlockY(), spot.getBlockZ());
                        if (isOrphanedLight(block)) lights.add(block);
                    }
                    return new Report(stores, barriers, lights, hitboxes, unknownItems);
                }, Bukkit.getScheduler().getMainThreadExecutor(GardensFurniture.plugin));
    }

    /** Removes what the report found, checking each thing again first. Returns how many were removed. Main thread. */
    public static int remove(Report report) {
        int removed = 0;
        for (Block block : report.stores()) {
            if (CustomBlockManager.isStoreBacked(block.getType())) continue;
            // not a break anybody made, so don't tell lifecycle listeners (Prism, GardensCrops) it was
            FurnitureStoreHandler.suppressLifecycle.set(true);
            try {
                FurnitureStoreHandler.INSTANCE.removeFurnitureStore(block);
            } finally {
                FurnitureStoreHandler.suppressLifecycle.set(false);
            }
            removed++;
        }
        for (Block block : report.barriers()) {
            if (!isOrphanedBarrier(block)) continue;
            block.setType(Material.AIR);
            removed++;
        }
        for (Block block : report.lights()) {
            if (!isOrphanedLight(block)) continue;
            block.setType(Material.AIR);
            removed++;
        }
        for (Interaction interaction : report.hitboxes()) {
            if (!interaction.isValid() || CustomBlockManager.getFurnitureEntity(interaction.getLocation(), false) != null) continue;
            interaction.remove();
            removed++;
        }
        return removed;
    }

    private static boolean within(Block block, int cx, int cy, int cz, int radius) {
        return Math.abs(block.getX() - cx) <= radius && Math.abs(block.getY() - cy) <= radius && Math.abs(block.getZ() - cz) <= radius;
    }

    private static boolean isOrphanedBarrier(Block block) {
        return block.getType() == Material.BARRIER && CustomBlockManager.getFurnitureEntity(block.getLocation()) == null;
    }

    private static boolean isOrphanedLight(Block block) {
        return block.getType() == Material.LIGHT && !LightUtil.isLitByFurniture(block);
    }

    // a display we placed (it carries our furniture tag) whose item the registry no longer knows
    private static boolean isOrphanedFurniture(ItemDisplay display) {
        var item = display.getItemStack();
        return item != null && !item.getType().isAir()
                && de.tr7zw.nbtapi.NBT.get(item, nbt -> (boolean) nbt.hasTag(com.gardensmc.gardensfurniture.register.ItemRegistry.FURNITURE_KEY))
                && GardensFurniture.itemRegistry.getCustomItem(item) == null;
    }
}
