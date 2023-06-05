package com.gardensmc.gardensfurniture.custom;

import org.bukkit.Material;

import java.util.List;

public class CustomCaveVineItem extends CustomItem {

    private CustomCaveVineBlock plant, vine, vine_25;

    public CustomCaveVineItem(String identifier, String display, int custom_model, Material material, List<String> lore, CustomCaveVineBlock plant, CustomCaveVineBlock vine, CustomCaveVineBlock vine_25) {
        super(identifier, display, custom_model, material, lore);
        this.plant = plant;
        this.vine = vine;
        this.vine_25 = vine_25;
    }

    public CustomCaveVineBlock getVine() {
        return vine;
    }

    public CustomCaveVineBlock getPlant() {
        return plant;
    }

    public CustomCaveVineBlock getVine_25() {
        if (vine_25 == null) {
            return vine;
        }
        return vine_25;
    }

    @Override
    public CustomBlock getCustomBlock() {
        return vine;
    }
}
