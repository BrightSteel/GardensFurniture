package com.gardensmc.gardensfurniture.itementity;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

import static com.gardensmc.gardensfurniture.register.ItemRegistry.FURNITURE_KEY;

public class DisplayItemEntity implements ItemEntity {

    private final ItemDisplay displayEntity;

    public DisplayItemEntity(ItemDisplay displayEntity) {
        this.displayEntity = displayEntity;
    }

    @Override
    public ItemStack getItem() {
        if (displayEntity.getItemStack().getType() != Material.AIR) {
            return displayEntity.getItemStack();
        }
        return NBT.getPersistentData(displayEntity, nbt -> nbt.getItemStack("DISPLAY_ITEM"));
    }

    @Override
    public Entity getEntity() {
        return displayEntity;
    }

    @Override
    public void setItem(ItemStack item) {
        displayEntity.setItemStack(item);
    }

    @Override
    public void rotate(int degrees) {
        displayEntity.setRotation(displayEntity.getYaw() + degrees, displayEntity.getPitch());
    }
}
