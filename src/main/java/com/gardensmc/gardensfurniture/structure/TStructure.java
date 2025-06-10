package com.gardensmc.gardensfurniture.structure;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.block.CustomLeavesBlock;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.item.PlaceOptions;
import com.gardensmc.gardensfurniture.store.FurnitureCache;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Utility class for easy implementation of structure gen from terra files (.tesf)
 * Provides useful block and furniture functions
 */
public abstract class TStructure {

    protected Location origin;
    private final Set<Chunk> furnitureAffectedChunks = new HashSet<>();

    public TStructure setOrigin(Location origin) {
        this.origin = origin;
        return this;
    }

    public abstract void generate();

    public void refreshChunks() {
        furnitureAffectedChunks.forEach(chunk -> FurnitureCache.INSTANCE.refreshChunk(new FurnitureCache.ChunkKey(chunk)));
    }

    protected void block(double x, double y, double z, String blockData) {
        if (origin == null) {
            throwOriginException();
        }
        Location loc = origin.clone().add(x, y, z);
        BlockData blockData1 = Bukkit.createBlockData(blockData);
        loc.getBlock().setBlockData(blockData1, false);
    }

    protected void furniture(double x, double y, double z, String identifier) {
        furniture(x, y, z, identifier, new PlaceOptions().setForce(true));
    }

    protected void furniture(double x, double y, double z, String identifier, BlockFace blockFace) {
        furniture(x, y, z, identifier, new PlaceOptions().setBlockFace(blockFace).setForce(true));
    }

    protected void furniture(double x, double y, double z, String identifier, PlaceOptions placeOptions) {
        if (origin == null) {
            throwOriginException();
        }
        Location loc = origin.clone().add(x, y, z);
        CustomItem customItem = GardensFurniture.itemRegistry.getItem(identifier);
        if (customItem instanceof CustomPlaceableItem placeableItem) {
            placeableItem.placeBlock(null, null, loc, placeOptions);
            furnitureAffectedChunks.add(loc.getChunk());
        }
    }

    protected boolean furnitureChecked(double x, double y, double z, String identifier) {
        var loc = origin.clone().add(x, y, z);
        if (origin.getWorld().getBlockAt(loc).getType().isSolid()) {
            return false;
        }
        furniture(x, y, z, identifier);
        return true;
    }

    protected int randomInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    protected double pow2(double num) {
        return  Math.pow(num, 2);
    }

    private void throwOriginException() {
        throw new RuntimeException(String.format("No origin set for structure %s, failed to generate properly", getClass().getSimpleName()));
    }
}
