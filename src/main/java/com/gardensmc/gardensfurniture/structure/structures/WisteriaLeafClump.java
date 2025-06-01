package com.gardensmc.gardensfurniture.structure.structures;

import com.gardensmc.gardensfurniture.store.FurnitureCache;
import com.gardensmc.gardensfurniture.structure.TStructure;
import com.gardensmc.gardensfurniture.util.ProbabilityUtil;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class WisteriaLeafClump extends TStructure {

    private final WisteriaTree.Color treeColor;
    private final Map<FurnitureCache.BlockKey,LeafState> leafStateMap = new HashMap<>();
    
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
            } else if (ProbabilityUtil.roll(50)) {
                leafState = LeafState.HALF_COLOR;
            }
        } else if (y == 0) {
            switch (leafStateMap.get(new FurnitureCache.BlockKey(x, y + 1, z))) {
                case HALF_COLOR -> {
                    leafState = LeafState.FULL_COLOR;
                    furnitureChecked(x, y - 1, z, getLeafIdentifier(LeafState.VINE));
                }
                case FULL_COLOR -> leafState = LeafState.VINE;
                case GREEN -> {
                    if (ProbabilityUtil.roll(33)) {
                        leafState = LeafState.HALF_COLOR;
                        furnitureChecked(x, y - 1, z, getLeafIdentifier(LeafState.FULL_COLOR));
                        furnitureChecked(x, y - 2, z, getLeafIdentifier(LeafState.VINE));
                    }
                }
            }
        }
        super.furniture(x, y, z, getLeafIdentifier(leafState));
        this.leafStateMap.put(new FurnitureCache.BlockKey(x, y, z), leafState);
    }

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
