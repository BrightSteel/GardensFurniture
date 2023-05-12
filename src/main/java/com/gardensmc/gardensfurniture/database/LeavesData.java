package com.gardensmc.gardensfurniture.database;

import com.gardensmc.gardensfurniture.GardensFurniture;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class LeavesData {

    public static LeavesData INSTANCE = new LeavesData();

    public void addLeave(int chunkX, int chunkZ, int x, int y, int z, String world, String identifier) throws SQLException {
        Block block = Bukkit.getWorld(world).getBlockAt(x, y, z);
        addLeave(block, identifier);
    }

    public void addLeave(Block block, String identifier) {
        Chunk chunk = block.getChunk();
        PersistentDataContainer data = chunk.getPersistentDataContainer();
        data.set(new NamespacedKey("gardens", block.getX() + "." + block.getY() + "." + block.getZ()),  PersistentDataType.STRING, identifier);
    }

    public void removeLeave(Block block) {

        Chunk chunk = block.getChunk();
        PersistentDataContainer data = chunk.getPersistentDataContainer();
        data.remove(new NamespacedKey("gardens", block.getX() + "." + block.getY() + "." + block.getZ()));
    }


}
