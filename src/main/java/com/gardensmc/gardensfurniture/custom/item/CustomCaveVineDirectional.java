package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomCaveVineBlock;
import com.gardensmc.gardensfurniture.util.DirectionUtil;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CustomCaveVineDirectional extends CustomPlaceableItem {

    private CustomCaveVineBlock vine_north, vine_south, vine_east, vine_west;

    public CustomCaveVineDirectional(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        this.vine_north = (CustomCaveVineBlock) GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("north"));
        this.vine_south = (CustomCaveVineBlock) GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("south"));
        this.vine_east = (CustomCaveVineBlock) GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("east"));
        this.vine_west = (CustomCaveVineBlock) GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("west"));
    }

    @Override
    public boolean placeBlock(Player player, ItemStack heldItem, Location location, PlaceOptions placeOptions) {
        if (!super.placeBlock(player, heldItem, location, placeOptions)) return false;
        BlockFace facing = BlockFace.valueOf(DirectionUtil.getDirectionFromFacing(player)).getOppositeFace();
        return vine_north.placeBlock(player, heldItem, this, location, placeOptions.setFacing(facing.name()));
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
