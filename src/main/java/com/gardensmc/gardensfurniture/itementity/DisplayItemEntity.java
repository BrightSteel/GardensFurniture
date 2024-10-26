package com.gardensmc.gardensfurniture.itementity;

import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

public class DisplayItemEntity implements ItemEntity {

    private final ItemDisplay displayEntity;

    public DisplayItemEntity(ItemDisplay displayEntity) {
        this.displayEntity = displayEntity;
    }

    @Override
    public ItemStack getItem() {
        return displayEntity.getItemStack();
    }

    @Override
    public Entity getEntity() {
        return displayEntity;
    }
}
