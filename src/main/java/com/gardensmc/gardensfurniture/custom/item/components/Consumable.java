package com.gardensmc.gardensfurniture.custom.item.components;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public class Consumable {

    private final double consume_seconds;
    private final String animation;

    public Consumable(ConfigurationSection consumableSection) {
        this.consume_seconds = consumableSection.getDouble("consume_seconds");
        this.animation = consumableSection.getString("animation");
    }
}
