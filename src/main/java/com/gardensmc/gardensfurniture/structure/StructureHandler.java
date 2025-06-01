package com.gardensmc.gardensfurniture.structure;

import com.gardensmc.gardensfurniture.structure.structures.*;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

public class StructureHandler {

    public static final StructureHandler INSTANCE = new StructureHandler();

    // structure_name -> structure
    private final Map<String, TStructure> structureMap;

    private StructureHandler() {
        structureMap = new HashMap<>();
        mapStructures();
    }

    public boolean spawnStructure(String structureName, Location location) {
        if (structureMap.containsKey(structureName)) {
            var structure = structureMap.get(structureName);
            structure.setOrigin(location);
            structure.generate();
            structure.refreshChunks();
            return true;
        }
        return false;
    }

    private void mapStructures() {
        structureMap.put("bee_nest", new BeeNest());
//        structureMap.put("maple_tree", new MapleTree());
        structureMap.put("palm_tree", new PalmTree());
//        structureMap.put("redwood_tree", new RedwoodTree());
        structureMap.put("eucalyptus_tree", new EucalyptusTree());
        structureMap.put("wisteria_tree", new WisteriaTree());
    }
}
