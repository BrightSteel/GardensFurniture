package com.gardensmc.gardensfurniture.hook;

import com.enderbeam.enderwardrobe.api.WardrobeApi;
import com.gardensmc.gardensfurniture.GardensFurniture;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * GardensFurniture's staffs, as EnderWardrobe offhand items: staffs.yml (items.yml's offhand shape there), so they get
 * EnderWardrobe's recipes, skins, wear and staff enchantments; and our items as recipe ingredients, gardens:id.
 * Only loaded when EnderWardrobe is.
 */
public final class WardrobeHook {

    private WardrobeHook() {
    }

    public static void register() {
        var api = Bukkit.getServicesManager().load(WardrobeApi.class);
        if (api == null) return;
        var plugin = GardensFurniture.plugin;
        var file = new File(plugin.getDataFolder(), "staffs.yml");
        if (!file.exists()) plugin.saveResource("staffs.yml", false);
        api.addItemResolver("gardens", id -> GardensFurniture.itemRegistry.getItem(id) == null ? null
                : GardensFurniture.itemRegistry.buildItem(id));
        api.addOffhandItems(plugin, () -> YamlConfiguration.loadConfiguration(file).getConfigurationSection("staffs"));
        plugin.getLogger().info("Staffs in staffs.yml handed to EnderWardrobe.");
    }
}
