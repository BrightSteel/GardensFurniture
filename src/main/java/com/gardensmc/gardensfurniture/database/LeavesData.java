package com.gardensmc.gardensfurniture.database;

import com.gardensmc.gardensfurniture.GardensFurniture;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class LeavesData {

    public static LeavesData INSTANCE = new LeavesData();

    public void addLeave(int chunkX, int chunkZ, int x, int y, int z, String world, String identifier) throws SQLException {

        world = world.toLowerCase();

        ensureTableExists(world);

        DatabaseConnector.INSTANCE.executeUpdate(
                String.format(
                        """
                        INSERT INTO %s (chunkX, chunkZ, x, y, z, identifier) VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT(x, y, z) DO UPDATE SET identifier = ?;
                        """, world, world
                ), chunkX, chunkZ, x, y, z, identifier, identifier
        );

    }

    public void addLeave(Block block, String identifier) throws SQLException {
        Chunk chunk = block.getChunk();
        addLeave(chunk.getX(), chunk.getZ(), block.getX(), block.getY(), block.getZ(), block.getWorld().getName(), identifier);
    }

    public void removeLeave(Block block) throws SQLException {
        DatabaseConnector.INSTANCE.executeUpdate(
                String.format("DELETE FROM %s WHERE x = ? AND y = ? AND z = ?", block.getWorld().getName().toLowerCase()),
                block.getX(), block.getY(), block.getZ()
        );
    }

    /**
     * Gets all leaves in a chunk
     * @param chunk
     * @return x,y,z -> leaveId
     */
    public HashMap<String, String> getLeavesInChunk(Chunk chunk) throws SQLException {
        String world = chunk.getWorld().getName().toLowerCase();

        ensureTableExists(world);

        Connection con = DatabaseConnector.INSTANCE.getConnection();
        String query = String.format(
                """ 
                SELECT x, y, z, identifier FROM %s WHERE chunkX = ? AND chunkZ = ?;
                """,
                world, world
        );
        PreparedStatement ps = con.prepareStatement(query);
        ps.setInt(1, chunk.getX());
        ps.setInt(2, chunk.getZ());

        HashMap<String, String> leavesMap = new HashMap<>();
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int x = rs.getInt(1);
            int y = rs.getInt(2);
            int z = rs.getInt(3);
            String identifier = rs.getString(4);
            leavesMap.put(
                    x + "," + y + "," + z,
                    identifier
            );
        }

        rs.close();
        ps.close();
//        con.close();
        return leavesMap;
    }

    private void ensureTableExists(String world) throws SQLException {
        DatabaseConnector.INSTANCE.executeUpdate(
                String.format(
                "CREATE TABLE IF NOT EXISTS %s (chunkX INT, chunkZ INT, x INT, y INT, z INT, identifier VARCHAR(64), PRIMARY KEY(x, y, z))", world));
    }





}
