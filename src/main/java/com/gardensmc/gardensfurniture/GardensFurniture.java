package com.gardensmc.gardensfurniture;

import com.gardensmc.gardensfurniture.command.CommandRegister;
import com.gardensmc.gardensfurniture.register.CustomRegister;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class GardensFurniture extends JavaPlugin {

    public static GardensFurniture plugin;

    @Override
    public void onEnable() {
        plugin = this;

        ConfigUtil.loadConfigs();
        CustomRegister.INSTANCE = new CustomRegister();

        CommandRegister.registerCommands();
    }
}
