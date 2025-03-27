package com.gardensmc.gardensfurniture.custom.item;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.logging.Level;

public class CustomPotionItem extends CustomItem {

    private boolean glint = true;
    private Color color;

    public CustomPotionItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        var potionSection = itemSection.getConfigurationSection("potion");
        if (potionSection != null) {
            this.glint = potionSection.getBoolean("glint", true);
            this.color = potionSection.contains("color") ? parseColor(potionSection.getString("color")) : null;
        }
    }

    public boolean isGlint() {
        return glint;
    }

    public Color getPotionColor() {
        return color;
    }

    private Color parseColor(String colors) {
        var split = colors.split(", ");
        if (split.length < 3) {
            Bukkit.getLogger().log(Level.WARNING, "");
            return null;
        }
        try {
            return Color.fromRGB(
                    Integer.parseInt(split[0]),
                    Integer.parseInt(split[1]),
                    Integer.parseInt(split[2])
            );
        } catch (NumberFormatException e) {
            Bukkit.getLogger().log(Level.WARNING, "Invalid color string for potion, ignoring: " + colors);
            return null;
        }
    }

    @Override
    public Material getMaterial() {
        return Material.POTION;
    }
}
