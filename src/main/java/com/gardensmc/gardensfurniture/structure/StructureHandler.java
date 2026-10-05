package com.gardensmc.gardensfurniture.structure;

import com.gardensmc.gardensfurniture.structure.structures.*;
import com.gardensmc.gardensfurniture.util.ProtectionUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class StructureHandler {

    public static final StructureHandler INSTANCE = new StructureHandler();

    // structure_name -> structure
    private final Map<String, Supplier<TStructure>> structureMap;

    private StructureHandler() {
        structureMap = new HashMap<>();
        mapStructures();
    }

    // radius checked for foreign claims before placing a datapack feature, which can't be filtered per block
    public static final int FEATURE_CLAIM_RADIUS = 16;

    public java.util.Set<String> getStructureNames() {
        return structureMap.keySet();
    }

    public boolean hasStructure(String structureName) {
        return structureMap.containsKey(structureName);
    }

    public boolean spawnStructure(String structureName, Location location) {
        return spawnStructure(structureName, location, false);
    }

    /**
     * Spawn a structure grown by a player (sapling, bone meal). Unlike world gen it only writes to cells
     * a tree could grow into and never crosses into someone else's claim.
     */
    public boolean spawnGrownStructure(String structureName, Location location) {
        return spawnStructure(structureName, location, true);
    }

    private boolean spawnStructure(String structureName, Location location, boolean grown) {
        if (structureMap.containsKey(structureName)) {
            var structure = structureMap.get(structureName).get();
            if (grown) {
                structure.setCellFilter(cell -> isGrowthReplaceable(cell.getBlock()) && ProtectionUtil.canGrowInto(location, cell));
            }
            structure.setOrigin(location);
            structure.generate();
            structure.refreshChunks();
            return true;
        }
        return false;
    }

    private void mapStructures() {
        structureMap.put("bee_nest", BeeNest::new);
//        structureMap.put("maple_tree", new MapleTree());
        structureMap.put("palm_tree", PalmTree::new);
//        structureMap.put("redwood_tree", new RedwoodTree());
        structureMap.put("eucalyptus_tree", EucalyptusTree::new);
        structureMap.put("wisteria_tree", WisteriaTree::new);
    }

    private static boolean isGrowthReplaceable(Block block) {
        var type = block.getType();
        return type.isAir() || type == Material.LIGHT
                || Tag.REPLACEABLE_BY_TREES.isTagged(type)
                || Tag.LEAVES.isTagged(type)
                || Tag.DIRT.isTagged(type); // dirt under the trunk
    }
}
