package com.gardensmc.gardensfurniture.custom.item;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class CustomItem {

    private final String identifier;
    private final String display;
    private final List<String> lore;
    private final int custom_model;
    private final Material material;
    private final boolean dyeable;
    private final String color;

    public CustomItem(String identifier, ConfigurationSection itemSection) {
        this.identifier = identifier;
        this.display = itemSection.getString("display");
        this.custom_model = itemSection.getInt("custom_model");
        this.material = parseMaterial(itemSection.getString("material"));
        this.lore = itemSection.getStringList("lore");
        this.dyeable = itemSection.getBoolean("dyeable");
        this.color = itemSection.getString("color");
    }

    public int getCustomModel() {
        return custom_model;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplay() {
        return display;
    }

    public List<String> getLore() {
        return lore;
    }

    public String getIdentifier() {
        return identifier;
    }

    public boolean isDyeable() {
        return dyeable;
    }

    public String getColor() {
        return color;
    }

    protected Material parseMaterial(String material_str) {
        Material material = Material.PAPER;
        if (material_str != null) {
            try {
                material = Material.valueOf(material_str.toUpperCase());
            } catch (Exception e) {
                return material;
            }
        }
        return material;
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }
}

