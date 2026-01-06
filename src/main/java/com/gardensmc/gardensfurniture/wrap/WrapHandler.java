package com.gardensmc.gardensfurniture.wrap;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomWrapItem;
import de.tr7zw.nbtapi.NBT;
import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.AllArgsConstructor;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
public class WrapHandler {

    private static final String WRAP_KEY = "FurnitureWrap";
    private static final String WRAP_ID_KEY = "WrapId";
    private static final String COLOR_KEY = "Color";

    private final ItemStack itemStack;

    public ItemStack wrapItem(CustomWrapItem wrapItem, Color color) {
        var clone = itemStack.clone();
        var originalColor = clone.getItemMeta() instanceof LeatherArmorMeta leatherMeta ? leatherMeta.getColor() : null;
        NBT.modify(clone, nbt -> {
            var wrapNBT = nbt.getOrCreateCompound(WRAP_KEY);
            wrapNBT.setString(WRAP_ID_KEY, wrapItem.getIdentifier());
            if (originalColor != null) {
             wrapNBT.setInteger(COLOR_KEY, originalColor.asRGB());
            }
        });
        var isNameDefault = isNameDefault();
        clone.editMeta(meta -> {
            meta.setItemModel(wrapItem.getWrapItemModel());
            if (isNameDefault) {
                meta.displayName(wrapItem.getDisplayName());
            }
            var components = wrapItem.getComponents();
            if (components != null) {
                if (components.getTooltip_style() != null) {
                    meta.setTooltipStyle(NamespacedKey.fromString(components.getTooltip_style()));
                }
                if (wrapItem.getWrapType() == CustomWrapItem.WrapType.ELYTRA) {
                    var equippableComponent = wrapItem.getComponents().getEquippable();
                    if (equippableComponent != null) {
                        var equippable = meta.getEquippable();
                        equippable.setSlot(equippableComponent.getSlot());
                        if (equippableComponent.getModel() != null) {
                            equippable.setModel(NamespacedKey.fromString(equippableComponent.getModel()));
                        }
                        meta.setEquippable(equippable);
                    }
                }
            }
        });

        if (clone.getItemMeta() instanceof LeatherArmorMeta leatherMeta && color != null) {
            leatherMeta.setColor(color);
            clone.setItemMeta(leatherMeta);
        }
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
        var wrap = getWrapFromItem();
        if (wrap != null) {
            if (wrap.getWrapType() == CustomWrapItem.WrapType.ELYTRA) {
                wrappedItem.resetData(DataComponentTypes.EQUIPPABLE);
            }
            if (wrap.getComponents() != null && wrap.getComponents().getTooltip_style() != null) {
                wrappedItem.resetData(DataComponentTypes.TOOLTIP_STYLE);
            }
        }
        // preserve original color
        var originalColor = NBT.get(wrappedItem, nbt -> {
            var compound = nbt.getCompound(WRAP_KEY);
            if (compound == null) {
                return null;
            }
            return compound.getInteger(COLOR_KEY);
        });
        if (originalColor != null && wrappedItem.getItemMeta() instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(Color.fromRGB(originalColor));
        }
        // remove wrapped nbt
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
