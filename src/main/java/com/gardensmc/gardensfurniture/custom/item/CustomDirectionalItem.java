package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockFactory;
import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;


public class CustomDirectionalItem extends CustomPlaceableItem {

    private CustomBlock x, y, z;

    public CustomDirectionalItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        this.x = GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("x"));
        this.y = GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("y"));
        this.z = GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("z"));
    }

    @Override
    public boolean placeBlock(ItemStack heldItem, Player player, Location location, BlockFace blockFace, boolean persistent) {
        if (!super.placeBlock(heldItem, player, location, blockFace, persistent)) return false;
        CustomBlock customBlock;
        switch (blockFace) {
                case UP, DOWN -> customBlock = getY();
                case EAST, WEST -> customBlock = getX();
                case NORTH, SOUTH -> customBlock = getZ();
                default -> throw new RuntimeException("Unknown block face: " + blockFace);
            }
        return customBlock.placeBlock(heldItem, this, player, location, blockFace, persistent, new FurnitureStore(getIdentifier()));
    }

    @Override
    public CustomBlock getCustomBlock() {
        return getX();
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
}
