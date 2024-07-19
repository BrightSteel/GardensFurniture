package com.gardensmc.gardensfurniture.store;

import de.tr7zw.nbtapi.NBTChunk;
import org.bukkit.Chunk;
import org.bukkit.block.Block;

import java.util.Objects;

public class FurnitureStoreHandler {

    public static FurnitureStoreHandler INSTANCE = new FurnitureStoreHandler();
    private static final String IDENTIFIER_KEY = "customItem";
    private static final String BLOCK_DATA_KEY = "furnitureStore";

    public void addFurnitureStore(Block block, FurnitureStore furnitureStore) {
        var nbtChunk = new NBTChunk(block.getChunk()).getPersistentDataContainer();
        var chunkBlockCompound = nbtChunk.getOrCreateCompound(getBlockKey(block));

        chunkBlockCompound.setString(IDENTIFIER_KEY, furnitureStore.getItemIdentifier());
        var blockData = chunkBlockCompound.addCompound(BLOCK_DATA_KEY);
        if (furnitureStore.getFacing() != null) {
            blockData.setString("facing", furnitureStore.getFacing());
        }
        if (furnitureStore.getMultiplier() != 0) {
            blockData.setInteger("multiplier", furnitureStore.getMultiplier());
        }
    }

    public void removeFurnitureStore(Block block) {
        var nbtChunk = new NBTChunk(block.getChunk()).getPersistentDataContainer();
        nbtChunk.removeKey(getBlockKey(block));
    }

    public FurnitureStore getFurnitureStore(Block block) {
        return getFurnitureStore(block.getChunk(), block.getX(), block.getY(), block.getZ());
    }

    public FurnitureStore getFurnitureStore(Chunk chunk, int x, int y, int z) {
        var nbtChunk = new NBTChunk(chunk).getPersistentDataContainer();
        var chunkBlockCompound = nbtChunk.getOrCreateCompound(getBlockKey(x, y, z));

        var customItem = chunkBlockCompound.getString(IDENTIFIER_KEY);
        var blockData = chunkBlockCompound.getCompound(BLOCK_DATA_KEY);
        // no blockData
        if (blockData == null) {
            return new FurnitureStore(customItem);
        }

        int multiplier = blockData.hasTag("multiplier") ? blockData.getInteger("multiplier") : 0;
        return new FurnitureStore(
                customItem,
                multiplier,
                blockData.getString("facing")
        );
    }

    private String getBlockKey(Block block) {
        return getBlockKey(block.getX(), block.getY(), block.getZ());
    }

    private String getBlockKey(int x, int y, int z) {
        return String.format("%s.%s.%s", x, y, z);
    }
}
