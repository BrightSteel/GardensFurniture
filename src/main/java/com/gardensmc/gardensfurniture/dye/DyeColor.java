package com.gardensmc.gardensfurniture.dye;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;

@Getter
@AllArgsConstructor
public class DyeColor {

    private final String name;
    private final String color;

    public DyeColor(ConfigurationSection dyeColorSection) {
        this.name = dyeColorSection.getString("name");
        this.color = dyeColorSection.getString("color");
    }

    public Color getBukkitColor() {
        var javaColor = java.awt.Color.decode(color);
        return Color.fromRGB(javaColor.getRed(), javaColor.getGreen(), javaColor.getBlue());
    }
}
