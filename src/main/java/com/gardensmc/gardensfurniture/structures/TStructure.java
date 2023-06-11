package com.gardensmc.gardensfurniture.structures;

import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.CustomItem;
import com.gardensmc.gardensfurniture.register.CustomRegister;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

/**
 * Utility class for easy implementation of structure gen from terra files (.tesf)
 * Provides useful block and furniture functions
 */
public abstract class TStructure {

    private final Location origin;

    public TStructure(Location origin) {
        this.origin = origin;
    }

    public abstract void generate();

    protected void block(double x, double y, double z, String blockData) {
        Location loc = origin.clone().add(x, y, z);
        BlockData blockData1 = Bukkit.createBlockData(blockData);
        loc.getBlock().setBlockData(blockData1, false);
    }

    protected void furniture(double x, double y, double z, String identifier) {
        Location loc = origin.clone().add(x, y, z);
        CustomItem customItem = CustomRegister.INSTANCE.itemRegistry.getItem(identifier);
        if (customItem == null) return;
        CustomBlockManager.attemptPlacement(null, customItem, loc, BlockFace.UP, false);
    }
}
