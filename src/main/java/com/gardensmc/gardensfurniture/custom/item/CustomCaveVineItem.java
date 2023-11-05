package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomCaveVineBlock;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

import static com.gardensmc.gardensfurniture.GardensFurniture.customBlockFactory;

public class CustomCaveVineItem extends CustomBlockItem {

    private CustomCaveVineBlock plant, vine, vine_25;

    public CustomCaveVineItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        this.plant = (CustomCaveVineBlock) customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("plant"));
        this.vine = (CustomCaveVineBlock) customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("vine"));
        this.vine_25 = (CustomCaveVineBlock) customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("vine_25"));
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

    @Override
    public CustomBlock getCustomBlock() {
        return vine;
    }
}
