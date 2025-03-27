package com.gardensmc.gardensfurniture.itementity;

import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

public interface ItemEntity {
    ItemStack getItem();
    Entity getEntity();
    void setItem(ItemStack item);
}
