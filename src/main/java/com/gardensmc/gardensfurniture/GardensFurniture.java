package com.gardensmc.gardensfurniture;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.gardensmc.gardensfurniture.command.CommandHandler;
import com.gardensmc.gardensfurniture.hook.HookGardensFurniture;
import com.gardensmc.gardensfurniture.listener.ListenerHandler;
import com.gardensmc.gardensfurniture.listener.sounds.DigSoundListener;
import com.gardensmc.gardensfurniture.mapletap.MapleTapHandler;
import com.gardensmc.gardensfurniture.register.ItemRegistry;
import com.gardensmc.gardensfurniture.structure.StructureHandler;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import me.lojosho.hibiscuscommons.hooks.Hooks;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class GardensFurniture extends JavaPlugin {

    public static GardensFurniture plugin;
    public static Plugin gardensPlayers;

    public static StructureHandler structureHandler;
    public static ItemRegistry itemRegistry;

    public static ProtocolManager protocolManager;

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
        protocolManager = ProtocolLibrary.getProtocolManager();
        new DigSoundListener().registerListener();

        // load custom item configs
        ConfigUtil.loadConfigs();

        // load registry
        itemRegistry = new ItemRegistry();
        itemRegistry.assembleItems();

        // register handlers
        CommandHandler.registerCommands();
        ListenerHandler.registerListeners();
        structureHandler = new StructureHandler();

        // init packet events
        PacketEvents.getAPI().init();

        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, MapleTapHandler.INSTANCE::tick, 0, 20);

        // hook into hmc cosmetics
        // this is kind of hacky...
        var hook = new HookGardensFurniture();
        hook.setDetected(true);
        hook.setActive(true);
        Hooks.addHook(hook);

//        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, ChunkFurnitureLoader.INSTANCE::tickChunkFurnitureLoader, 0, 1);
//        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, ChunkFurnitureLoader.INSTANCE::tickChunkStats, 0, 100);
    }
}
