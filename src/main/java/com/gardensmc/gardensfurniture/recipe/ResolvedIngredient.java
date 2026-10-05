package com.gardensmc.gardensfurniture.recipe;

import org.bukkit.Material;

import java.util.Set;
import java.util.function.Predicate;

/** An ingredient with its tags expanded: the vanilla materials and GardensFurniture ids that fit. */
public record ResolvedIngredient(Set<Material> materials, Set<String> customIds) implements Predicate<ItemKey> {

    public ResolvedIngredient {
        materials = Set.copyOf(materials);
        customIds = Set.copyOf(customIds);
    }

    @Override
    public boolean test(ItemKey item) {
        if (item == null) return false;
        return item.isCustom() ? customIds.contains(item.customId()) : materials.contains(item.material());
    }

    public boolean isEmpty() {
        return materials.isEmpty() && customIds.isEmpty();
    }
}
