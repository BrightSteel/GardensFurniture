package com.gardensmc.gardensfurniture.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.annotation.Nullable;
import java.io.File;
import java.util.HashMap;

public class LeavesConfig {

    @Nullable
    public static HashMap<String, String> getChunkData(Chunk chunk) {
        // x,y,z -> leaveId
        HashMap<String, String> data = new HashMap<>();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(getLeavesFile(chunk.getWorld()));
        ConfigurationSection section = config.getConfigurationSection(chunk.getX() + "," + chunk.getZ());
        if (section == null) {
            return null;
        }
        for (String key : section.getKeys(false)) {
            data.put(key, section.getString(key));
        }
        return data;
    }

    public static void addLeave(Block block, String leaveId) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(getLeavesFile(block.getWorld()));
        ConfigurationSection section = config.getConfigurationSection(block.getChunk().getX() + "," + block.getChunk().getZ());
        if (section == null) {
            section = config.createSection(block.getChunk().getX() + "," + block.getChunk().getZ());
        }

        section.set(block.getX() + "," + block.getY() + "," + block.getZ(), leaveId);
        try {
            config.save(getLeavesFile(block.getWorld()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        GardensFurniture.leavesMap.get(block.getWorld().getName()).refreshChunk(block.getChunk());
    }

    public static void removeLeave(Block block) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(getLeavesFile(block.getWorld()));
        ConfigurationSection section = config.getConfigurationSection(block.getChunk().getX() + "," + block.getChunk().getZ());
        if (section == null) return;

        section.set(block.getX() + "," + block.getY() + "," + block.getZ(), null);
        try {
            config.save(getLeavesFile(block.getWorld()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        GardensFurniture.leavesMap.get(block.getWorld().getName()).refreshChunk(block.getChunk());
    }



    private static File getLeavesFile(World world) {
        File file = new File(world.getWorldFolder(), "leaves.yml");
        try {
            file.createNewFile(); // create file if not exists
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }
}
