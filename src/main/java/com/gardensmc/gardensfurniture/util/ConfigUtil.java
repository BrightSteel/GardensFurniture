package com.gardensmc.gardensfurniture.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigUtil {

    public static Configuration itemsYaml, glyphYaml;

    public static void loadConfigs() {
        File itemFile = getYMLFile("items.yml");
        itemsYaml = YamlConfiguration.loadConfiguration(itemFile);

        File glyphFile = getYMLFile("glyphs.yml");
        glyphYaml = YamlConfiguration.loadConfiguration(glyphFile);
    }

    public static File getYMLFile(String file_name) {
        File file = new File(GardensFurniture.plugin.getDataFolder(), file_name);
        try {
            file.mkdirs();
            file.createNewFile(); // create file if not exists
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

}
