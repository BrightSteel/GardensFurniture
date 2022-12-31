package com.gardensmc.gardensfurniture;

import com.gardensmc.gardensfurniture.command.FurnitureCommand;
import com.gardensmc.gardensfurniture.command.GetCommand;
import com.gardensmc.gardensfurniture.command.NbtCommand;
import com.gardensmc.gardensfurniture.items.GlyphRegistry;
import com.gardensmc.gardensfurniture.items.ItemRegistry;
import com.gardensmc.gardensfurniture.items.Registries;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class GardensFurniture extends JavaPlugin {

    public static GardensFurniture plugin;

    @Override
    public void onEnable() {
        plugin = this;

        ConfigUtil.loadConfigs();
        Registries.INSTANCE = new Registries();

        Objects.requireNonNull(getCommand("nbt")).setExecutor(new NbtCommand());
        Objects.requireNonNull(getCommand("get")).setExecutor(new GetCommand());
        Objects.requireNonNull(getCommand("furniture")).setExecutor(new FurnitureCommand());
    }
}
