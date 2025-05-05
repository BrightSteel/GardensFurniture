package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.item.PlaceOptions;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CustomCarpetBlock extends CustomDisplayBlock {

    public CustomCarpetBlock(ConfigurationSection blockSection, String identifier) {
        super(blockSection, identifier);
        this.hitbox = Hitbox.NONE;
    }

    @Override
    public boolean placeBlock(Player player, ItemStack heldItem, CustomPlaceableItem customItem, Location location, PlaceOptions placeOptions) {
        if (placeOptions.getBlockFace() != BlockFace.UP) return false;

        boolean success = super.placeBlock(player, heldItem, customItem, location, placeOptions);
        if (success) {
            var block = location.getBlock();
            block.setType(Material.BROWN_CARPET);
        }
        return success;
    }
}
