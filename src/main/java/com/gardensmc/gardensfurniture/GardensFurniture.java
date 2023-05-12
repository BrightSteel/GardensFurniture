package com.gardensmc.gardensfurniture;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.gardensmc.gardensfurniture.command.CommandRegister;
import com.gardensmc.gardensfurniture.events.Events;
import com.gardensmc.gardensfurniture.listener.S2CListener;
import com.gardensmc.gardensfurniture.register.CustomRegister;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class GardensFurniture extends JavaPlugin {

    public static GardensFurniture plugin;
    private ProtocolManager protocolManager;

    @Override
    public void onEnable() {
        plugin = this;

        ConfigUtil.loadConfigs();
        CustomRegister.INSTANCE = new CustomRegister();

        this.getServer().getPluginManager().registerEvents(new Events(), this);

        CommandRegister.registerCommands();
        protocolManager = ProtocolLibrary.getProtocolManager();
        new S2CListener(protocolManager);
    }


}
