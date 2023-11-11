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


public class CustomCaveVineItem extends CustomPlaceableItem {

    private CustomCaveVineBlock plant, vine, vine_25;

    public CustomCaveVineItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        ConfigurationSection blockSection = itemSection.getConfigurationSection("block");
        this.plant = (CustomCaveVineBlock) GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("plant"));
        this.vine = (CustomCaveVineBlock) GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("vine"));
        this.vine_25 = (CustomCaveVineBlock) GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("vine_25"));
    }

    @Override
    public boolean placeBlock(Player player, Location location, BlockFace blockFace, boolean persistent) {
        return plant.placeBlock(this, player, location, blockFace, persistent);
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
