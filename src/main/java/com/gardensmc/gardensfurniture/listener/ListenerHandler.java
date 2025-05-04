package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.listener.sounds.VanillaBlockSoundListener;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.util.*;

public class ListenerHandler {

    public static List<Listener> listeners;

    public static void registerListeners() {
        createListeners();
        for (Listener listener : listeners) {
            Bukkit.getServer().getPluginManager().registerEvents(listener, GardensFurniture.plugin);
        }
        // packet listeners
        new BlockPacketListener().register();
    }

    private static void createListeners() {
        listeners = List.of(
                new BlockListener(),
                new CaveVineListener(),
                new ChunkListener(),
                new EntityListener(),
                new LeafListener(),
                new PlayerListener(),
                new CustomBlockBreakListener(),
                new VanillaBlockSoundListener()
        );
    }
}
