package com.gardensmc.gardensfurniture.custom;

import org.bukkit.Material;

import java.util.List;

public class CustomItem {

    private final String display;
    private final List<String> lore;
    private final int custom_model;
    private final Material material;

    public CustomItem(String display, int custom_model, Material material, List<String> lore) {
        this.display = display;
        this.custom_model = custom_model;
        this.material = material;
        this.lore = lore;
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
}

