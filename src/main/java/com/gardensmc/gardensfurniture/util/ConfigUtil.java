package com.gardensmc.gardensfurniture.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigUtil {

    public static Map<String, Configuration> all_items = new HashMap<>();
    public static Configuration generationMappings;

    public static FileConfiguration itemsYaml, itemSaverYaml;

    public static void loadConfigs() {
//        File itemFile = getYMLFile("items.yml");
//        itemsYaml = YamlConfiguration.loadConfiguration(itemFile);
        all_items.clear();
        all_items = getAllConfigs("items");

        File itemSaverFile = getYMLFile("item_saver.yml");
        itemSaverYaml = YamlConfiguration.loadConfiguration(itemSaverFile);

        File generationMappingFile = getYMLFile("generation_mappings.yml");
        generationMappings = YamlConfiguration.loadConfiguration(generationMappingFile);
    }

    public static void saveItemSaverYaml() {
        try {
            File itemSaverFile = getYMLFile("item_saver.yml");
            itemSaverYaml.save(itemSaverFile);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void reloadSavedItems() {
        File itemSaverFile = getYMLFile("item_saver.yml");
        itemSaverYaml = YamlConfiguration.loadConfiguration(itemSaverFile);
    }

    public static Map<String, Configuration> getAllConfigs(String directory) {
        File file = new File(GardensFurniture.plugin.getDataFolder(), directory);
        File[] sub_files = file.listFiles();

        Map<String, Configuration> configs = new HashMap<>();
        for (File sub : sub_files) {
            if (!sub.getName().contains(".DS_Store")) {
                configs.put(sub.getName(), YamlConfiguration.loadConfiguration(sub));
            }
        }

        return configs;

    }

    public static File getYMLFile(String file_name) {
        File file = new File(GardensFurniture.plugin.getDataFolder(), file_name);
        try {
            file.getParentFile().mkdirs();
            file.createNewFile(); // create file if not exists
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

}
