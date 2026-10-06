package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.block.CustomLeavesBlock;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.util.CustomDropUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.LeavesDecayEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LeafListener implements Listener {

    @EventHandler
    public void leafDecay(LeavesDecayEvent e) {
        if (e.isCancelled()) return;

        Block leafBlock = e.getBlock();
        if (leafBlock.getBlockData() instanceof Leaves leaf) {
            var customItem = CustomBlockManager.getItemFromLeaves(leafBlock);
            if (!(customItem instanceof CustomPlaceableItem placeable)) return;
            if (!preventDecay(leafBlock, leaf)) {
                CustomBlockManager.removeCustomBlock(leafBlock);
                Bukkit.getScheduler().runTaskLater(GardensFurniture.plugin, () -> CustomDropUtil.leafDrop(null, leafBlock.getLocation().add(0.5, 0.1, 0.5), (CustomLeavesBlock) placeable.getCustomBlock()), 1);
            }
            e.setCancelled(true);
        }
    }

    // Recently found supporting logs. Leaves of one tree decay-check in bursts, so the log that held up the
    // last leaf usually holds up the next one too - checking these first skips most of the scan.
    private final Deque<Block> recentSupports = new ArrayDeque<>();
    private static final int MAX_RECENT_SUPPORTS = 32;
    // offsets of the search cube per decay distance, nearest first, so a nearby log ends the search early
    private static final Map<Integer, int[][]> OFFSETS_BY_DISTANCE = new ConcurrentHashMap<>();

    // A custom leaf is kept while any note block (custom logs are note blocks) is inside the cube around it.
    private boolean preventDecay(Block leafBlock, Leaves leaf) {
        int distance = leaf.getDistance();
        for (var support : recentSupports) {
            if (support.getWorld().equals(leafBlock.getWorld()) && inCube(leafBlock, support, distance)
                    && support.getType() == Material.NOTE_BLOCK) {
                return true;
            }
        }
        for (int[] offset : offsets(distance)) {
            Block b = leafBlock.getRelative(offset[0], offset[1], offset[2]);
            if (b.getType() == Material.NOTE_BLOCK) {
                recentSupports.addFirst(b);
                if (recentSupports.size() > MAX_RECENT_SUPPORTS) {
                    recentSupports.removeLast();
                }
                return true;
            }
        }
        return false;
    }

    // same bounds as the original scan: -distance inclusive to distance exclusive on each axis
    private static boolean inCube(Block center, Block other, int distance) {
        int dx = other.getX() - center.getX(), dy = other.getY() - center.getY(), dz = other.getZ() - center.getZ();
        return dx >= -distance && dx < distance && dy >= -distance && dy < distance && dz >= -distance && dz < distance;
    }

    private static int[][] offsets(int distance) {
        return OFFSETS_BY_DISTANCE.computeIfAbsent(distance, d -> {
            var offsets = new ArrayList<int[]>();
            for (int x = -d; x < d; x++) {
                for (int y = -d; y < d; y++) {
                    for (int z = -d; z < d; z++) {
                        offsets.add(new int[]{x, y, z});
                    }
                }
            }
            offsets.sort(Comparator.comparingInt(o -> Math.max(Math.abs(o[0]), Math.max(Math.abs(o[1]), Math.abs(o[2])))));
            return offsets.toArray(new int[0][]);
        });
    }
}
