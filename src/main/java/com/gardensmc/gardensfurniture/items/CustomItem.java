package com.gardensmc.gardensfurniture.items;

import org.bukkit.Material;

public class CustomItem {

    private String identifier;
    private String display;
    private String lore;
    private int custom_model;
    private Material material;

    public CustomItem(String identifier, String display, int custom_model) {
        this.identifier = identifier;
        this.display = display;
        this.custom_model = custom_model;
        this.material = Material.PAPER;
    }

    public String getIdentifier() {
        return identifier;
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

    public String getLore() {
        return lore;
    }
}

