package com.gardensmc.gardensfurniture.custom.item;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public abstract class CustomPlaceableItem extends CustomItem {

    public CustomPlaceableItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
    }

    public abstract boolean placeBlock(Player player, Location location, BlockFace blockFace, boolean persistent);
}
