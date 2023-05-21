package com.gardensmc.gardensfurniture.custom;

import org.bukkit.Material;

import java.util.List;

public class CustomDirectionalItem extends CustomItem {

    private CustomBlock x, y, z;

    public CustomDirectionalItem(String identifier, String display, int custom_model, Material material, List<String> lore, CustomBlock x, CustomBlock y, CustomBlock z) {
        super(identifier, display, custom_model, material, lore);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public CustomBlock getX() {
        return x;
    }

    public CustomBlock getY() {
        return y;
    }

    public CustomBlock getZ() {
        return z;
    }

    @Override
    public CustomBlock getCustomBlock() {
        return y;
    }
}
