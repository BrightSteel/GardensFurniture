package com.gardensmc.gardensfurniture.custom;

import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomCaveVineBlock;
import com.gardensmc.gardensfurniture.custom.item.*;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;

public class CustomItemFactory {

    public CustomItem createCustomItem(String identifier, ConfigurationSection itemSection) {
        CustomItem customItem;
        switch (getCustomItemType(itemSection)) {
            case BLOCK -> customItem = new CustomBlockItem(identifier, itemSection);
            case DIRECTIONAL_BLOCK -> customItem = new CustomDirectionalItem(identifier, itemSection);
            case CAVE_VINE_BLOCK -> customItem = new CustomCaveVineItem(identifier, itemSection);
            case CAVE_VINE_DIRECTIONAL_BLOCK -> customItem = new CustomCaveVineDirectional(identifier, itemSection);
            default -> customItem = new CustomItem(identifier, itemSection);
        }
        return customItem;
    }

    private CustomItemType getCustomItemType(ConfigurationSection blockSection) {
        if (blockSection != null) {
            String blockType = blockSection.getString("type");
            if (blockType != null) {
                return CustomItemType.valueOf(blockType.toUpperCase());
            }
            return CustomItemType.BLOCK;
        }
        return CustomItemType.ITEM;
    }
}
