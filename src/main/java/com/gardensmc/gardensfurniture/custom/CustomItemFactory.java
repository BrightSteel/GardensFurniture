package com.gardensmc.gardensfurniture.custom;

import com.gardensmc.gardensfurniture.custom.item.*;
import com.gardensmc.gardensfurniture.custom.type.CustomItemType;
import org.bukkit.configuration.ConfigurationSection;

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

    private CustomItemType getCustomItemType(ConfigurationSection itemSection) {
        String itemType = itemSection.getString("type");
        if (itemType != null) {
            return CustomItemType.valueOf(itemType.toUpperCase());
        }
        return CustomItemType.ITEM;
    }
}
