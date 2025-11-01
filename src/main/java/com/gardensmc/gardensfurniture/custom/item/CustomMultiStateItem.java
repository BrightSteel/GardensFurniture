package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import org.bukkit.configuration.ConfigurationSection;

public class CustomMultiStateItem extends CustomPlaceableItem {

    public CustomMultiStateItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
    }

    @Override
    public CustomBlock getCustomBlock() {
        return null;
    }
}
