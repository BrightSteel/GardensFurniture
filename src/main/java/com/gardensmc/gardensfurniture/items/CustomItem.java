package com.gardensmc.gardensfurniture.items;

import org.bukkit.Material;

import java.util.List;

public class CustomItem {

    private String display;
    private List<String> lore;
    private int custom_model;
    private Material material;

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

