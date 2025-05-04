package com.gardensmc.gardensfurniture.generator.biomes;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public class AddSettings {

    private final int count;
    private final RangedValue continentalness;
    private final double depth;
    private final RangedValue erosion;
    private final RangedValue humidity;
    private final RangedValue temperature;
    private final RangedValue weirdness;

    public AddSettings(ConfigurationSection settings) {
        this.count = settings.getInt("count");
        this.continentalness = new RangedValue(settings.getConfigurationSection("continentalness"));
        this.depth = settings.getDouble("depth");
        this.erosion = new RangedValue(settings.getConfigurationSection("erosion"));
        this.humidity = new RangedValue(settings.getConfigurationSection("humidity"));
        this.temperature = new RangedValue(settings.getConfigurationSection("temperature"));
        this.weirdness = new RangedValue(settings.getConfigurationSection("weirdness"));
    }

}
