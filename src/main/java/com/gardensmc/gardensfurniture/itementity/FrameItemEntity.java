package com.gardensmc.gardensfurniture.itementity;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
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

    @Override
    public void rotate(int degrees) {
        itemFrame.setRotation(itemFrame.getYaw() + degrees, itemFrame.getPitch());
    }

}
