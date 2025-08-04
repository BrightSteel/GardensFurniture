package com.gardensmc.gardensfurniture.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class BiomeUtil {

    public static String getBiomeKey(Location loc) {
        return Bukkit.getUnsafe().getBiomeKey(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()).getKey();
    }

}
