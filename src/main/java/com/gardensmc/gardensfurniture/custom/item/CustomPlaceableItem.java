package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public abstract class CustomPlaceableItem extends CustomItem {

    public CustomPlaceableItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
    }

    public abstract boolean placeBlock(Player player, Location location, BlockFace blockFace, boolean persistent);

    // used to get general properties of the block, if multiple blocks return main one
    public abstract CustomBlock getCustomBlock();
}
