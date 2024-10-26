package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CustomCarpetBlock extends CustomDisplayBlock {

    public CustomCarpetBlock(ConfigurationSection blockSection) {
        super(blockSection);
    }

    @Override
    public boolean placeBlock(ItemStack heldItem, CustomPlaceableItem placeableItem, Player player, Location location, BlockFace blockFace, boolean persistent, FurnitureStore furnitureStore) {
        if (blockFace != BlockFace.UP) return false;

        boolean success = super.placeBlock(heldItem, placeableItem, player, location, blockFace, persistent, furnitureStore);
        if (success) {
            var block = location.getBlock();
            block.setType(Material.BROWN_CARPET);
        }
        return success;
    }
}
