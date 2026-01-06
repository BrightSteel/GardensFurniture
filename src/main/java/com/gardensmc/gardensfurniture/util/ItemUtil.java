package com.gardensmc.gardensfurniture.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ItemUtil {

    public static boolean isEmpty(ItemStack itemStack) {
        return itemStack == null || itemStack.getType() == Material.AIR || itemStack.isEmpty();
    }

}
