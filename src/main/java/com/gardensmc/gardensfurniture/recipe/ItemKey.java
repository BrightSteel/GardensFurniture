package com.gardensmc.gardensfurniture.recipe;

import org.bukkit.Material;

import javax.annotation.Nullable;

/**
 * What a recipe cares about in an item stack: its GardensFurniture id when it has one, otherwise its
 * material. A custom item is never treated as its base material, so a quilt (paper underneath) never
 * counts as paper.
 */
public record ItemKey(Material material, @Nullable String customId) {

    public static ItemKey vanilla(Material material) {
        return new ItemKey(material, null);
    }

    public static ItemKey custom(Material material, String customId) {
        return new ItemKey(material, customId);
    }

    public boolean isCustom() {
        return customId != null;
    }
}
