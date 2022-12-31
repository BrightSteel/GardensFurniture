package com.gardensmc.gardensfurniture;

import com.gardensmc.gardensfurniture.command.NbtCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class GardensFurniture extends JavaPlugin {


    public static GardensFurniture plugin;


    @Override
    public void onEnable() {
        plugin = this;

        Objects.requireNonNull(getCommand("nbt")).setExecutor(new NbtCommand());
    }
}
