package com.gardensmc.gardensfurniture.generator.biomes;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

@Getter
public class CustomBiome {

    private final String biomeName;
    private final Modifier modifier;
    private ReplaceSettings replaceSettings;
    private AddSettings addSettings;

    public CustomBiome(String biomeName, ConfigurationSection section) {
        this.biomeName = biomeName;
        this.modifier = Modifier.valueOf(Objects.requireNonNull(section.getString("modifier")).toUpperCase());
        var settings = section.getConfigurationSection("settings");
        if (modifier == Modifier.ADD) {
            addSettings = new AddSettings(Objects.requireNonNull(settings));
        } else {
            replaceSettings = new ReplaceSettings(Objects.requireNonNull(settings));
        }
    }

    public enum Modifier {
        ADD,
        REPLACE
    }
}
