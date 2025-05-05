package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import com.gardensmc.gardensfurniture.util.DirectionUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class CustomFacingItem extends CustomPlaceableItem {

    private final CustomBlock north;
    private final CustomBlock south;
    private final CustomBlock east;
    private final CustomBlock west;

    public CustomFacingItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        this.north = GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("north"));
        this.south = GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("south"));
        this.east = GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("east"));
        this.west = GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("west"));
    }

    @Override
    public boolean placeBlock(Player player, ItemStack heldItem, Location location, PlaceOptions placeOptions) {
        if (!super.placeBlock(player, heldItem, location, placeOptions)) return false;
        BlockFace facing = getFaceForPlacement(player, location);
       return getCustomBlock(facing.name()).placeBlock(
               player,
               heldItem,
               this,
               location,
               placeOptions.setFacing(facing.name())
       );
    }

    private BlockFace getFaceForPlacement(Player player, Location location) {
        if (player != null) {
            return BlockFace.valueOf(DirectionUtil.getDirectionFromFacing(player)).getOppositeFace();
        } else {
            // placed by terrain - lets put it against a block if possible since used by log mushroom
            for (var blockFace : Set.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)) {
                var rel = location.getBlock().getRelative(blockFace);
                if (rel.getType() == Material.BIRCH_LOG) { // .isBlock includes air, grass, plants, etc, imma just do this for now
                    return blockFace.getOppositeFace();
                }
            }
        }
        return BlockFace.NORTH; // default
    }

    @Override
    public CustomBlock getCustomBlock() {
        return getNorth();
    }

    public CustomBlock getCustomBlock(String facing) {
        if (facing.equalsIgnoreCase("NORTH")) {
            return north;
        } else if (facing.equalsIgnoreCase("SOUTH")) {
            return south;
        } else if (facing.equalsIgnoreCase("EAST")) {
            return east;
        } else if (facing.equalsIgnoreCase("WEST")) {
            return west;
        }
        throw new RuntimeException("Failed to get custom block for facing: " + facing);
    }

    public CustomBlock getNorth() {
        return north;
    }

    public CustomBlock getSouth() {
        return south;
    }

    public CustomBlock getEast() {
        return east;
    }

    public CustomBlock getWest() {
        return west;
    }
}
