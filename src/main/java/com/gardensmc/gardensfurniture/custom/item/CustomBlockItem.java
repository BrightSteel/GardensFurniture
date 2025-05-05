package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomWaxedBlock;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class CustomBlockItem extends CustomPlaceableItem {

    private final CustomBlock customBlock;

    public CustomBlockItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        this.customBlock = GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection);
        if (this.customBlock instanceof CustomWaxedBlock customWaxedBlock) {
            this.material = customWaxedBlock.getWaxedType().getMaterial();
        }
    }

    @Override
    public CustomBlock getCustomBlock() {
        return customBlock;
    }

    @Override
    public boolean placeBlock(Player player, ItemStack heldItem, Location location, PlaceOptions placeOptions) {
        if (!super.placeBlock(player, heldItem, location, placeOptions)) return false;
        return customBlock.placeBlock(player, heldItem, this, location, placeOptions);
    }
}
