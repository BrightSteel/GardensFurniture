package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Getter
public class CustomSaplingBlock extends CustomDisplayBlock {

    public CustomSaplingBlock(ConfigurationSection blockSection, String identifier) {
        super(blockSection, identifier);
        this.hitbox = Hitbox.NONE;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public boolean placeBlock(ItemStack heldItem, CustomPlaceableItem placeableItem, Player player, Location location, BlockFace blockFace, boolean persistent, FurnitureStore furnitureStore) {
        return false; // handle placement naturally with event
    }

    public void placeDisplayEntity(CustomPlaceableItem customItem, Location location, BlockFace blockFace) {
        super.placeDisplay(GardensFurniture.itemRegistry.buildItem(customItem), null, location, blockFace, null);
    }
}
