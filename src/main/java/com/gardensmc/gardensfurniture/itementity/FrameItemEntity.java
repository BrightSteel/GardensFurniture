package com.gardensmc.gardensfurniture.itementity;

import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;

public class FrameItemEntity implements ItemEntity {

    private final ItemFrame itemFrame;

    public FrameItemEntity(ItemFrame itemFrame) {
        this.itemFrame = itemFrame;
    }

    @Override
    public ItemStack getItem() {
        return itemFrame.getItem();
    }

    @Override
    public Entity getEntity() {
        return itemFrame;
    }

    @Override
    public void setItem(ItemStack item) {
        itemFrame.setItem(item);
    }
}
