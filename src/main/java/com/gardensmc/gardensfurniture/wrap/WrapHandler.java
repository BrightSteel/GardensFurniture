package com.gardensmc.gardensfurniture.wrap;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomWrapItem;
import de.tr7zw.nbtapi.NBT;
import lombok.AllArgsConstructor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
public class WrapHandler {

    private static final String WRAP_KEY = "FurnitureWrap";
    private static final String WRAP_ID_KEY = "WrapId";

    private final ItemStack itemStack;

    public ItemStack wrapItem(CustomWrapItem wrapItem) {
        var clone = itemStack.clone();
        NBT.modify(clone, nbt -> {
            nbt.getOrCreateCompound(WRAP_KEY).setString(WRAP_ID_KEY, wrapItem.getIdentifier());
        });
        var isNameDefault = isNameDefault();
        clone.editMeta(meta -> {
            meta.setItemModel(wrapItem.getWrapItemModel());
            if (isNameDefault) {
                meta.displayName(wrapItem.getDisplayName());
            }
        });
        return clone;
    }

    private boolean isNameDefault() {
        var customItem = GardensFurniture.itemRegistry.getCustomItem(itemStack);
        if (customItem != null) {
            var original = customItem.getDisplayName();
            var current = itemStack.getItemMeta().customName();
            return original.equals(current);
        }
        return !itemStack.getItemMeta().hasCustomName();
    }

    @Nullable
    public CustomWrapItem getWrapFromItem() {
        var wrapId = NBT.get(itemStack, nbt -> {
            var compound = nbt.getCompound(WRAP_KEY);
            if (compound == null) {
                return null;
            }
            return compound.getString(WRAP_ID_KEY);
        });
        return wrapId == null ? null : (CustomWrapItem) GardensFurniture.itemRegistry.getItem(wrapId);
    }

    @Nullable
    // null if item not wrapped
    public UnwrappedResult getUnwrappedResult() {
        var itemClone = itemStack.clone();
        var wrapId = removeWrapNBT(itemClone);
        if (wrapId == null) {
            return null;
        }
        resetItemModel(itemClone);
        return new UnwrappedResult(itemClone, (CustomWrapItem) GardensFurniture.itemRegistry.getItem(wrapId));
    }

    private void resetItemModel(ItemStack item) {
        var customItem = GardensFurniture.itemRegistry.getCustomItem(item);
        var itemModel = customItem == null || customItem.getComponents() == null
                ? null
                : NamespacedKey.fromString(customItem.getComponents().getItem_model());
        item.editMeta(meta -> meta.setItemModel(itemModel));
    }

    @Nullable
    // removes wrap nbt and returns wrapped id, or null if item was not wrapped
    private String removeWrapNBT(ItemStack wrappedItem) {
        return NBT.modify(wrappedItem, nbt -> {
            var compound = nbt.getCompound(WRAP_KEY);
            if (compound == null) {
                return null;
            }
            var id = compound.getString(WRAP_ID_KEY);
            nbt.removeKey(WRAP_KEY);
            return id;
        });
    }

    public record UnwrappedResult(ItemStack unwrappedItem, CustomWrapItem wrapItem) {
    }
}
