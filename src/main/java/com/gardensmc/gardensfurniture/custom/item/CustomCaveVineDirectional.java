package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomCaveVineBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;


public class CustomCaveVineDirectional extends CustomPlaceableItem {

    private CustomCaveVineBlock vine_north, vine_south, vine_east, vine_west;

    public CustomCaveVineDirectional(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        ConfigurationSection blockSection = itemSection.getConfigurationSection("block");
        this.vine_north = (CustomCaveVineBlock) GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("north"));
        this.vine_south = (CustomCaveVineBlock) GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("south"));
        this.vine_east = (CustomCaveVineBlock) GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("east"));
        this.vine_west = (CustomCaveVineBlock) GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("west"));
    }

    @Override
    public boolean placeBlock(Player player, Location location, BlockFace blockFace, boolean persistent) {
        return vine_north.placeBlock(this, player, location, blockFace, persistent);
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
}
