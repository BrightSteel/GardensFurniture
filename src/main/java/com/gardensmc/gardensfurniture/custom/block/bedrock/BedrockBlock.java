package com.gardensmc.gardensfurniture.custom.block.bedrock;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.List;

@Getter
public class BedrockBlock {

    private final String geometry;
    private final List<MaterialInstance> materialInstances;
    private final List<Integer> rotation; // size of 3

    public BedrockBlock(ConfigurationSection bedrockSection) {
        this.geometry = bedrockSection.getString("geometry");
        var materialSection = bedrockSection.getConfigurationSection("material_instances");
        materialInstances = materialSection == null ? Collections.emptyList() : materialSection.getKeys(false)
                .stream()
                .map(key -> new MaterialInstance(key, materialSection.getString(key)))
                .toList();
        var rotations = bedrockSection.getIntegerList("rotation");
        if (!rotations.isEmpty() && rotations.size() != 3) {
            throw new IllegalStateException("Rotations must have length of 3!");
        }
        this.rotation = rotations;
    }
}
