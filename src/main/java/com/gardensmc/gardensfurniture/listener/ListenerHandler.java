package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.mapletap.MapleTapHandler;
import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.listener.sounds.VanillaBlockSoundListener;
import com.gardensmc.gardensfurniture.menu.ChatPrompt;
import com.gardensmc.gardensfurniture.recipe.CustomFuelHandler;
import com.gardensmc.gardensfurniture.recipe.FurnaceInputRefresher;
import com.gardensmc.gardensfurniture.recipe.RecipeListener;
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
                new ChunkListener(),
                new EntityListener(),
                new LeafListener(),
                new PlayerListener(),
                new CustomBlockBreakListener(),
                new VanillaBlockSoundListener(),
                new RecipeListener(),
                CustomFuelHandler.INSTANCE,
                new FurnaceInputRefresher(),
                ChatPrompt.INSTANCE,
                new BlockBreakingListener(),
                new EnderStepListener(),
                MapleTapHandler.INSTANCE
        );
    }
}
