package com.gardensmc.gardensfurniture;

import com.gardensmc.gardensfurniture.command.CommandRegister;
import com.gardensmc.gardensfurniture.events.CaveVineEvents;
import com.gardensmc.gardensfurniture.events.Events;
import com.gardensmc.gardensfurniture.listener.BlockPacketListener;
import com.gardensmc.gardensfurniture.register.CustomRegister;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class GardensFurniture extends JavaPlugin {

    public static GardensFurniture plugin;
    public static Plugin gardensPlayers;

    public static CustomRegister customRegister;

    @Override
    public void onLoad() {
        loadPacketEvents();
    }

    private void loadPacketEvents() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder
                .build(this, new PacketEventsSettings().checkForUpdates(false)));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        plugin = this;
        gardensPlayers = getServer().getPluginManager().getPlugin("GardensPlayers");

        ConfigUtil.loadConfigs();
        customRegister = new CustomRegister();

        CommandRegister.registerCommands();
        registerListeners();

        PacketEvents.getAPI().init();
    }

    private void registerListeners() {
        new BlockPacketListener().register();
        getServer().getPluginManager().registerEvents(new Events(), this);
        getServer().getPluginManager().registerEvents(new CaveVineEvents(), this);
    }


}
