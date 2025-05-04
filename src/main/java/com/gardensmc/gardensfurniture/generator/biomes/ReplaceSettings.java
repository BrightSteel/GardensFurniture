package com.gardensmc.gardensfurniture.generator.biomes;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public class ReplaceSettings {

    private final String vanilla;
    private final int count;
    private Double depthFilter;

    public ReplaceSettings(ConfigurationSection settings) {
        this.vanilla = settings.getString("vanilla");
        this.count = settings.getInt("count");
        var filters = settings.getConfigurationSection("filters");
        if (filters != null) {
            if (filters.contains("depth")) {
                this.depthFilter = filters.getDouble("depth");
            }
        }
    }
}
