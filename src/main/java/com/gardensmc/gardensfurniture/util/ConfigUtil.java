package com.gardensmc.gardensfurniture.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConfigUtil {

    public static List<Configuration> all_items = new ArrayList<>();

    public static Configuration itemsYaml, glyphYaml;

    public static void loadConfigs() {
//        File itemFile = getYMLFile("items.yml");
//        itemsYaml = YamlConfiguration.loadConfiguration(itemFile);
        all_items.clear();
        all_items = getAllConfigs("items");

        File glyphFile = getYMLFile("glyphs.yml");
        glyphYaml = YamlConfiguration.loadConfiguration(glyphFile);
    }

    public static List<Configuration> getAllConfigs(String directory) {
        File file = new File(GardensFurniture.plugin.getDataFolder(), directory);
        File[] sub_files = file.listFiles();

        List<Configuration> configs = new ArrayList<>();
        for (File sub : sub_files) {
            configs.add(YamlConfiguration.loadConfiguration(sub));
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
