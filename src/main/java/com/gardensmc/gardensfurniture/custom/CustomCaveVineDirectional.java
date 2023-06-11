package com.gardensmc.gardensfurniture.custom;

import org.bukkit.Material;

import java.util.List;

public class CustomCaveVineDirectional extends CustomItem {

    private CustomCaveVineBlock vine_north, vine_south, vine_east, vine_west;

    public CustomCaveVineDirectional(String identifier, String display, int custom_model, Material material, List<String> lore, CustomCaveVineBlock vine_north, CustomCaveVineBlock vine_south, CustomCaveVineBlock vine_east, CustomCaveVineBlock vine_west) {
        super(identifier, display, custom_model, material, lore);
        this.vine_north = vine_north;
        this.vine_south = vine_south;
        this.vine_east = vine_east;
        this.vine_west = vine_west;
    }

    public CustomCaveVineBlock getVine_north() {
        return vine_north;
    }

    public CustomCaveVineBlock getVine_east() {
        return vine_east;
    }

    public CustomCaveVineBlock getVine_south() {
        return vine_south;
    }

    public CustomCaveVineBlock getVine_west() {
        return vine_west;
    }

    @Override
    public CustomBlock getCustomBlock() {
        return vine_north;
    }
}
