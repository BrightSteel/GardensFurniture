package com.gardensmc.gardensfurniture.store;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class FurnitureStore {

    public static FurnitureStore INSTANCE = new FurnitureStore();

    public void addFurnitureStore(Block block, String identifier) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer data = chunk.getPersistentDataContainer();
        data.set(new NamespacedKey("gardens", block.getX() + "." + block.getY() + "." + block.getZ()),  PersistentDataType.STRING, identifier);
    }

    public void removeFurnitureStore(Block block) {

        Chunk chunk = block.getChunk();
        PersistentDataContainer data = chunk.getPersistentDataContainer();
        data.remove(new NamespacedKey("gardens", block.getX() + "." + block.getY() + "." + block.getZ()));
    }


}
