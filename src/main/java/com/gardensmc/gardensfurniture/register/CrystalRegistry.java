package com.gardensmc.gardensfurniture.register;

import com.gardensmc.gardensfurniture.custom.block.CustomCrystalBlock;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CrystalRegistry {

    // crystal_id -> (crystal type -> item identifier)
    public static final Map<String, Map<CustomCrystalBlock.CrystalType, String>> crystalSetMap = new HashMap<>();
    private final Map<CustomCrystalBlock.CrystalType, Map<Integer, String>> modelOverrides = new HashMap<>();

    @Nullable
    public String getCrystalIdentifier(String crystalId, CustomCrystalBlock.CrystalType crystalType) {
        var crystalSet = crystalSetMap.get(crystalId);
        if (crystalSet != null) {
            return crystalSet.get(crystalType);
        }
        return null;
    }

    @Nullable
    public String getCrystalIdentifier(CustomCrystalBlock.CrystalType crystalType, int modelOverride) {
        var overrides = modelOverrides.get(crystalType);
        if (overrides == null) return null;
        return overrides.get(modelOverride);
    }

    public void addCrystalToMap(CustomCrystalBlock crystalBlock, String identifier) {
        crystalSetMap.computeIfAbsent(crystalBlock.getCrystalId(), s -> new HashMap<>()).put(crystalBlock.getType(), identifier);
        modelOverrides.computeIfAbsent(crystalBlock.getType(), s -> new HashMap<>()).put(crystalBlock.getModelOverride(), identifier);
    }

    public void clear() {
        crystalSetMap.clear();
        modelOverrides.clear();
    }
}
