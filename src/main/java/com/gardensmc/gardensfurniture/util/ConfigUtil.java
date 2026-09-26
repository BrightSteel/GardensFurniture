package com.gardensmc.gardensfurniture.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigUtil {

    public static Map<String, Configuration> all_items = new HashMap<>();
    public static Configuration generationMappings;
    public static Configuration dyeColors;

    public static FileConfiguration itemsYaml, itemSaverYaml;

    public static void loadConfigs() {
        // config.yml: written from the jar's defaults on first start, re-read on /furniture reload
        GardensFurniture.plugin.saveDefaultConfig();
        GardensFurniture.plugin.reloadConfig();
        SpecialItems.load(GardensFurniture.plugin.getConfig().getConfigurationSection("special_items"));

        all_items.clear();
        all_items = getAllConfigs("items");

        File itemSaverFile = getYMLFile("item_saver.yml");
        itemSaverYaml = YamlConfiguration.loadConfiguration(itemSaverFile);

        File generationMappingFile = getYMLFile("generation_mappings.yml");
        generationMappings = YamlConfiguration.loadConfiguration(generationMappingFile);
        dyeColors = YamlConfiguration.loadConfiguration(getYMLFile("dye_colors.yml"));
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
            var name = sub.getName();
            var suffix = ".yml";
            if (name.endsWith(suffix)) {
                configs.put(name.substring(0, name.lastIndexOf(suffix)), YamlConfiguration.loadConfiguration(sub));
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
