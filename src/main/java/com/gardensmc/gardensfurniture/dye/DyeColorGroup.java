package com.gardensmc.gardensfurniture.dye;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Objects;

@Getter
public class DyeColorGroup {

    private final DyeColor baseColor;
    private final List<DyeColor> subColors;

    public DyeColorGroup(ConfigurationSection groupSection) {
        this.baseColor = new DyeColor(Objects.requireNonNull(groupSection.getConfigurationSection("baseColor")));
        this.subColors = groupSection.getMapList("subColors")
                .stream()
                .map(map -> new DyeColor((String) map.get("name"), (String) map.get("color")))
                .toList();
    }
}
