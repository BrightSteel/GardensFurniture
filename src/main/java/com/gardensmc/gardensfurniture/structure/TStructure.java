package com.gardensmc.gardensfurniture.structure;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Utility class for easy implementation of structure gen from terra files (.tesf)
 * Provides useful block and furniture functions
 */
public abstract class TStructure {

    protected Location origin;

    public TStructure setOrigin(Location origin) {
        this.origin = origin;
        return this;
    }

    public abstract void generate();

    protected void block(double x, double y, double z, String blockData) {
        if (origin == null) {
            throwOriginException();
        }
        Location loc = origin.clone().add(x, y, z);
        BlockData blockData1 = Bukkit.createBlockData(blockData);
        loc.getBlock().setBlockData(blockData1, false);
    }

    protected void furniture(double x, double y, double z, String identifier) {
        if (origin == null) {
            throwOriginException();
        }
        Location loc = origin.clone().add(x, y, z);
        CustomItem customItem = GardensFurniture.customRegister.itemRegistry.getItem(identifier);
        if (customItem == null) return;
        CustomBlockManager.attemptPlacement(null, customItem, loc, BlockFace.UP, false);
    }

    protected int randomInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    private void throwOriginException() {
        throw new RuntimeException(String.format("No origin set for structure %s, failed to generate properly", getClass().getSimpleName()));
    }
}
