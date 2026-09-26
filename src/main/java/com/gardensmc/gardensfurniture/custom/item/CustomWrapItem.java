package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.GardensFurniture;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.function.Predicate;

@Getter
public class CustomWrapItem extends CustomItem {

    private final NamespacedKey wrapItemModel;
    private final String cosmeticId;
    private final WrapType wrapType;

    public CustomWrapItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        var wrapModel = itemSection.getString("wrap_item_model");
        this.wrapItemModel = wrapModel != null ? NamespacedKey.fromString(wrapModel) : null;
        this.cosmeticId = itemSection.getString("cosmetic_id");
        this.wrapType = WrapType.valueOf(Objects.requireNonNull(itemSection.getString("wrap_type")).toUpperCase());
    }

    public NamespacedKey getWrapItemModel() {
        if (wrapItemModel != null) {
            return wrapItemModel;
        }
        return NamespacedKey.fromString(getComponents().getItem_model());
    }

    @AllArgsConstructor
    public enum WrapType {
        BACKPACK(itemStack -> GardensFurniture.itemRegistry.getCustomItem(itemStack) instanceof CustomBackpackItem),
        SWORD(itemStack -> isType(itemStack, "sword")),
        PICKAXE(itemStack -> isType(itemStack, "pickaxe")),
        AXE(itemStack -> isType(itemStack, "axe") && !isType(itemStack, "pickaxe")),
        SHOVEL(itemStack -> isType(itemStack, "shovel")),
        HOE(itemStack -> isType(itemStack, "hoe")),
        BOW(itemStack -> isType(itemStack, "bow")),
        CROSSBOW(itemStack -> isType(itemStack, "crossbow")),
        ELYTRA(itemStack -> isType(itemStack, "elytra")),
        FISHING_ROD(itemStack -> isType(itemStack, "fishing_rod")),
        MACE(itemStack -> isType(itemStack, "mace")),
        SHIELD(itemStack -> isType(itemStack, "shield"));

        public final Predicate<ItemStack> isWrappable;
    }

    private static boolean isType(ItemStack itemStack, String suffix) {
        return itemStack.getType().toString().toLowerCase().endsWith(suffix);
    }
}
