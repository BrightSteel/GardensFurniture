package com.gardensmc.gardensfurniture.custom.item.components;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public class Food {

    private final int nutrition;
    private final double saturation;
    private final boolean can_always_eat;

    public Food(ConfigurationSection foodSection) {
        this.nutrition = foodSection.getInt("nutrition");
        this.saturation = foodSection.getDouble("saturation");
        this.can_always_eat = foodSection.getBoolean("can_always_eat");
    }
}
