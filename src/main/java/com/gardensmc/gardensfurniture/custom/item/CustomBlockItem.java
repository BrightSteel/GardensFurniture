package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class CustomBlockItem extends CustomPlaceableItem {

    private final CustomBlock customBlock;
    protected final ConfigurationSection blockSection;

    public CustomBlockItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        this.blockSection = itemSection.getConfigurationSection("block");
        this.customBlock = GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection);
    }

    @Override
    public CustomBlock getCustomBlock() {
        return customBlock;
    }

    @Override
    public boolean placeBlock(ItemStack heldItem, Player player, Location location, BlockFace blockFace, boolean persistent) {
        return customBlock.placeBlock(heldItem, this, player, location, blockFace, persistent, new FurnitureStore(getIdentifier()));
    }
}
