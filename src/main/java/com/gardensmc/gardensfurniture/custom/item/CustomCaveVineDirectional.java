package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomCaveVineBlock;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

import static com.gardensmc.gardensfurniture.GardensFurniture.customBlockFactory;

public class CustomCaveVineDirectional extends CustomBlockItem {

    private CustomCaveVineBlock vine_north, vine_south, vine_east, vine_west;

    public CustomCaveVineDirectional(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        this.vine_north = (CustomCaveVineBlock) customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("north"));
        this.vine_south = (CustomCaveVineBlock) customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("south"));
        this.vine_east = (CustomCaveVineBlock) customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("east"));
        this.vine_west = (CustomCaveVineBlock) customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("west"));
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
