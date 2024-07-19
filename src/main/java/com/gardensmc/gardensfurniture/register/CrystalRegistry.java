package com.gardensmc.gardensfurniture.register;

import com.gardensmc.gardensfurniture.custom.block.CustomCrystalBlock;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CrystalRegistry {

    // crystal_id -> (crystal type -> item identifier)
    public static final Map<String, Map<CustomCrystalBlock.CrystalType, String>> crystalSetMap = new HashMap<>();

    @Nullable
    public String getCrystalIdentifier(String crystalId, CustomCrystalBlock.CrystalType crystalType) {
        var crystalSet = crystalSetMap.get(crystalId);
        if (crystalSet != null) {
            return crystalSet.get(crystalType);
        }
        return null;
    }

    public void addCrystalToMap(String crystalId, CustomCrystalBlock.CrystalType crystalType, String identifier) {
        crystalSetMap.computeIfAbsent(crystalId, s -> new HashMap<>()).put(crystalType, identifier);
    }

    public void clear() {
        crystalSetMap.clear();
    }
}
