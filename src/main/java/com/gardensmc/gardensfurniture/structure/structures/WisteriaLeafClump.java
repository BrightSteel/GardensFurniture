package com.gardensmc.gardensfurniture.structure.structures;

import com.gardensmc.gardensfurniture.store.FurnitureCache;
import com.gardensmc.gardensfurniture.structure.TStructure;
import com.gardensmc.gardensfurniture.util.ProbabilityUtil;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

@AllArgsConstructor
public class WisteriaLeafClump extends TStructure {

    private final WisteriaTree.Color treeColor;
    private final Map<FurnitureCache.BlockKey, LeafState> leafStateMap = new HashMap<>();
    private final Queue<FurniturePlacer> queuedVines = new LinkedList<>();
    
    @Override
    public void generate() {
        // placed top to bottom
        for (var y = 3; y >= 0; y--) {
            placeLeaf(0, y, 0);
            placeLeaf(0, y, 1);
            placeLeaf(1, y, 0);
            placeLeaf(1, y, 1);
        }
        for (var y = 2; y >= 1; y--) {
            placeLeaf(2, y, 1);
            placeLeaf(1, y, 2);
            placeLeaf(0, y, 2);
            placeLeaf(2, y, 0);
            placeLeaf(-1, y, 0);
            placeLeaf(-1, y, 1);
            placeLeaf(0, y, -1);
            placeLeaf(1, y, -1);
        }
        queuedVines.forEach(queuedVine -> {
            var loc = origin.add(queuedVine.blockKey.x(), queuedVine.blockKey.y(), queuedVine.blockKey.z());
            var topBlock = loc.getWorld().getBlockAt(loc);
            // avoid placing underneath log since those won't get replaced by half leaves
            if (topBlock.getType().toString().toLowerCase().endsWith("leaves")) {
                var placed = furnitureChecked(queuedVine.blockKey.x(), queuedVine.blockKey.y(), queuedVine.blockKey.z(), queuedVine.blockId);
                if (placed && queuedVine.blockId.contains("vine")) {
                    for (int i = 1; i < ThreadLocalRandom.current().nextInt(1, 4); i++) {
                        if (!furnitureChecked(queuedVine.blockKey.x(), queuedVine.blockKey.y() - i, queuedVine.blockKey.z(), queuedVine.blockId)) {
                            break;
                        }
                    }
                }
            }
        });
    }

    private void placeLeaf(int x, int y, int z) {
        var leafState = LeafState.GREEN;
        if (y == 2) {
            if (ProbabilityUtil.roll(33)) {
                leafState = LeafState.HALF_COLOR;
            }
        } else if (y == 1) {
            if (leafStateMap.get(new FurnitureCache.BlockKey(x, y + 1, z)) == LeafState.HALF_COLOR) {
                leafState = LeafState.FULL_COLOR;
                queuedVines.add(new FurniturePlacer(new FurnitureCache.BlockKey(x, y - 1, z), getLeafIdentifier(LeafState.VINE)));
            } else if (ProbabilityUtil.roll(50)) {
                leafState = LeafState.HALF_COLOR;
                queuedVines.add(new FurniturePlacer(new FurnitureCache.BlockKey(x, y - 1, z), getLeafIdentifier(LeafState.FULL_COLOR)));
                if (ProbabilityUtil.roll(50)) {
                    queuedVines.add(new FurniturePlacer(new FurnitureCache.BlockKey(x, y - 2, z), getLeafIdentifier(LeafState.VINE)));
                }
            }
        } else if (y == 0) {
            switch (leafStateMap.get(new FurnitureCache.BlockKey(x, y + 1, z))) {
                case HALF_COLOR -> {
                    leafState = LeafState.FULL_COLOR;
                    // need to place this vine after we place this leaf so it doesn't place in air and fail
                    queuedVines.add(new FurniturePlacer(new FurnitureCache.BlockKey(x, y - 1, z), getLeafIdentifier(LeafState.VINE)));
                }
                case FULL_COLOR -> {
                    if (ProbabilityUtil.roll(50)) {
                        queuedVines.add(new FurniturePlacer(new FurnitureCache.BlockKey(x, y - 2, z), getLeafIdentifier(LeafState.VINE)));
                    }
                    return;
                }
                case GREEN -> {
                    if (ProbabilityUtil.roll(33)) {
                        leafState = LeafState.HALF_COLOR;
                        queuedVines.add(new FurniturePlacer(new FurnitureCache.BlockKey(x, y - 1, z), getLeafIdentifier(LeafState.FULL_COLOR)));
                        queuedVines.add(new FurniturePlacer(new FurnitureCache.BlockKey(x, y - 2, z), getLeafIdentifier(LeafState.VINE)));
                    }
                }
            }
        }
        furnitureChecked(x, y, z, getLeafIdentifier(leafState));
        this.leafStateMap.put(new FurnitureCache.BlockKey(x, y, z), leafState);
    }

    private record FurniturePlacer(FurnitureCache.BlockKey blockKey, String blockId) {}

    private String getLeafIdentifier(LeafState leafState) {
        return switch (leafState) {
            case GREEN -> "wisteria_leaves";
            case HALF_COLOR -> treeColor.getLeavesHalf();
            case FULL_COLOR -> treeColor.getLeaves();
            case VINE -> treeColor.getVine();
        };
    }

    private enum LeafState {
        GREEN,
        HALF_COLOR,
        FULL_COLOR,
        VINE
    }
}
