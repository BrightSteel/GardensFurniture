package com.gardensmc.gardensfurniture.custom;

import org.bukkit.Material;

import java.util.List;

public class CustomItem {

    private final String identifier;

    private final String display;
    private final List<String> lore;
    private final int custom_model;
    private final Material material;

    private CustomBlock customBlock = CustomBlock.EMPTY;

    public CustomItem(String identifier, String display, int custom_model, Material material, List<String> lore) {
        this.identifier = identifier;
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

    public boolean isCustomBlock() {
        return customBlock != CustomBlock.EMPTY || this instanceof CustomDirectionalItem || this instanceof CustomCaveVineDirectional;
    }

    public CustomBlock getCustomBlock() {
        return customBlock;
    }

    public void setCustomBlock(CustomBlock customBlock) {
        this.customBlock = customBlock;
    }

    public String getIdentifier() {
        return identifier;
    }
}

