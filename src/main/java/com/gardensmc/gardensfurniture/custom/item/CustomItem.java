package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class CustomItem {

    private final String identifier;
    private final String display;
    private final List<String> lore;
    private final int custom_model;
    private final Material material;

    public CustomItem(String identifier, ConfigurationSection itemSection) {
        this.identifier = identifier;
        this.display = itemSection.getString("display");
        this.custom_model = itemSection.getInt("custom_model");
        this.material = parseMaterial(itemSection.getString("material"));
        this.lore = itemSection.getStringList("lore");
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
}

