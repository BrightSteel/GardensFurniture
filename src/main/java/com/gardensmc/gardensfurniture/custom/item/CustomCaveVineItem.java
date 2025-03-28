package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomCaveVineBlock;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class CustomCaveVineItem extends CustomPlaceableItem {

    private CustomCaveVineBlock plant, vine, vine_25;

    public CustomCaveVineItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        this.plant = (CustomCaveVineBlock) GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("plant"));
        this.vine = (CustomCaveVineBlock) GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("vine"));
        this.vine_25 = (CustomCaveVineBlock) GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("vine_25"));
    }

    @Override
    public boolean placeBlock(ItemStack heldItem, Player player, Location location, BlockFace blockFace, boolean persistent) {
        if (!super.placeBlock(heldItem, player, location, blockFace, persistent)) return false;
        return plant.placeBlock(heldItem, this, player, location, blockFace, persistent, new FurnitureStore(getIdentifier()));
    }

    @Override
    public CustomBlock getCustomBlock() {
        return plant;
    }

    public CustomCaveVineBlock getVine() {
        return vine;
    }

    public CustomCaveVineBlock getPlant() {
        return plant;
    }

    public CustomCaveVineBlock getVine_25() {
        if (vine_25 == null) {
            return vine;
        }
        return vine_25;
    }
}
