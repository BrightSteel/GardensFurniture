package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import org.bukkit.configuration.ConfigurationSection;

import static com.gardensmc.gardensfurniture.GardensFurniture.customBlockFactory;

public class CustomBlockItem extends CustomItem {

    private final CustomBlock customBlock;
    protected final ConfigurationSection blockSection;

    public CustomBlockItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        this.blockSection = itemSection.getConfigurationSection("block");
        this.customBlock = customBlockFactory.createCustomBlock(identifier, blockSection);
    }

    public CustomBlock getCustomBlock() {
        return customBlock;
    }
}
