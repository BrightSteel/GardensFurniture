package com.gardensmc.gardensfurniture.util;

import de.tr7zw.nbtapi.NBT;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ItemUtil {

    public static boolean isEmpty(ItemStack itemStack) {
        return itemStack == null || itemStack.getType() == Material.AIR || itemStack.isEmpty();
    }


    // EnderWardrobe's mark on an item with a skin applied
    private static final NamespacedKey SKIN_APPLIED = new NamespacedKey("enderwardrobe", "applied");

    /**
     * Whether a skin is applied to the item, by EnderWardrobe or (before it had skins) by this plugin, which wrote a
     * FurnitureWrap compound.
     */
    public static boolean isSkinned(ItemStack item) {
        if (isEmpty(item)) return false;
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(SKIN_APPLIED, PersistentDataType.TAG_CONTAINER)) {
            return true;
        }
        return NBT.get(item, (java.util.function.Function<de.tr7zw.nbtapi.iface.ReadableItemNBT, Boolean>) nbt -> nbt.hasTag("FurnitureWrap"));
    }
}
