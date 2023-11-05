package com.gardensmc.gardensfurniture;

import com.gardensmc.gardensfurniture.command.CommandHandler;
import com.gardensmc.gardensfurniture.listener.CaveVineListener;
import com.gardensmc.gardensfurniture.listener.BlockPacketListener;
import com.gardensmc.gardensfurniture.listener.ListenerHandler;
import com.gardensmc.gardensfurniture.register.CustomRegister;
import com.gardensmc.gardensfurniture.structure.StructureHandler;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class GardensFurniture extends JavaPlugin {

    public static GardensFurniture plugin;
    public static Plugin gardensPlayers;

    public static StructureHandler structureHandler;
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

        // load custom items
        ConfigUtil.loadConfigs();
        customRegister = new CustomRegister();

        // register handlers
        CommandHandler.registerCommands();
        ListenerHandler.registerListeners();
        structureHandler = new StructureHandler();

        // init packet events
        PacketEvents.getAPI().init();
    }
}
