package com.gardensmc.gardensfurniture.store;

import de.tr7zw.nbtapi.NBTChunk;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public class FurnitureStoreHandler {

    public static FurnitureStoreHandler INSTANCE = new FurnitureStoreHandler();
    public static final String IDENTIFIER_KEY = "customItem";
    public static final String BLOCK_DATA_KEY = "furnitureStore";

    public void addFurnitureStore(Block block, FurnitureStore furnitureStore, boolean refresh) {
        var nbtChunk = new NBTChunk(block.getChunk()).getPersistentDataContainer().getOrCreateCompound("GardensFurniture");
        var chunkBlockCompound = nbtChunk.getOrCreateCompound(getBlockKey(block));

        chunkBlockCompound.setString(IDENTIFIER_KEY, furnitureStore.getItemIdentifier());
        var blockData = chunkBlockCompound.addCompound(BLOCK_DATA_KEY);
        if (furnitureStore.getFacing() != null) {
            blockData.setString("facing", furnitureStore.getFacing());
        }
        if (furnitureStore.getMultiplier() != 0) {
            blockData.setInteger("multiplier", furnitureStore.getMultiplier());
        }
        if (refresh) {
            FurnitureCache.INSTANCE.refreshChunk(new FurnitureCache.ChunkKey(block.getWorld().getName(), block.getChunk().getX(), block.getChunk().getZ()));
        }
    }

    public void removeFurnitureStore(Block block) {
        var nbtChunk = new NBTChunk(block.getChunk()).getPersistentDataContainer().getOrCreateCompound("GardensFurniture");
        nbtChunk.removeKey(getBlockKey(block));
        FurnitureCache.INSTANCE.refreshChunk(new FurnitureCache.ChunkKey(block.getWorld().getName(), block.getChunk().getX(), block.getChunk().getZ()));
    }

    @Nullable
    public FurnitureStore getFurnitureStore(Block block) {
        return getFurnitureStore(block.getChunk(), block.getX(), block.getY(), block.getZ());
    }

    @Nullable
    public FurnitureStore getFurnitureStore(Chunk chunk, int x, int y, int z) {
        return FurnitureCache.INSTANCE.getFurnitureStore(
                new FurnitureCache.ChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()),
                new FurnitureCache.BlockKey(x, y, z)
        );
    }

    private String getBlockKey(Block block) {
        return getBlockKey(block.getX(), block.getY(), block.getZ());
    }

    private String getBlockKey(int x, int y, int z) {
        return String.format("%s.%s.%s", x, y, z);
    }
}
