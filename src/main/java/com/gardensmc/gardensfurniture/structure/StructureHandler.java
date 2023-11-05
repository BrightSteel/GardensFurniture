package com.gardensmc.gardensfurniture.structure;

import com.gardensmc.gardensfurniture.structure.structures.BeeNest;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

public class StructureHandler {

    // structure_name -> structure
    private final Map<String, TStructure> structureMap;

    public StructureHandler() {
        structureMap = new HashMap<>();
        mapStructures();
    }

    public boolean spawnStructure(String structureName, Location location) {
        if (structureMap.containsKey(structureName)) {
            structureMap.get(structureName)
                    .setOrigin(location)
                    .generate();
            return true;
        }
        return false;
    }

    private void mapStructures() {
        structureMap.put("BEE_NEST", new BeeNest());
    }
}
