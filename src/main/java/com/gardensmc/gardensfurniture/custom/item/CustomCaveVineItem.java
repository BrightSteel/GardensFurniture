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

    private final CustomCaveVineBlock plant;
    private final CustomCaveVineBlock vine;
    private final CustomCaveVineBlock vine_25;

    public CustomCaveVineItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        this.plant = (CustomCaveVineBlock) GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("plant"));
        this.vine = (CustomCaveVineBlock) GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("vine"));
        this.vine_25 = blockSection.contains("vine_25")
                ? (CustomCaveVineBlock) GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("vine_25"))
                : null;
    }

    @Override
    public boolean placeBlock(Player player, ItemStack heldItem, Location location, PlaceOptions placeOptions) {
        if(!super.placeBlock(player, heldItem, location, placeOptions)) {
            return false;
        }
        return plant.placeBlock(player, heldItem, this, location, placeOptions);
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