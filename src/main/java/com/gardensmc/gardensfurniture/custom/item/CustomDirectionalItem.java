package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockFactory;
import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

import static com.gardensmc.gardensfurniture.GardensFurniture.customBlockFactory;

public class CustomDirectionalItem extends CustomBlockItem {

    private CustomBlock x, y, z;

    public CustomDirectionalItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        this.x = customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("x"));
        this.y = customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("y"));
        this.z = customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("z"));
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

    @Override
    public CustomBlock getCustomBlock() {
        return y;
    }
}
