package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.GardensFurniture;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public record WrappedItem(ItemStack itemStack, CustomItem customItem) {

    public static Optional<WrappedItem> create(ItemStack itemStack) {
        var customItem = GardensFurniture.itemRegistry.getCustomItem(itemStack);
        if (customItem == null) {
            return Optional.empty();
        }
        return Optional.of(new WrappedItem(itemStack, customItem));
    }
}
