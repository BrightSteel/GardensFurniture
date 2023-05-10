package com.gardensmc.gardensfurniture.command;

import com.gardensmc.gardensfurniture.GardensFurniture;

import java.util.Objects;

public class CommandRegister {

    public static void registerCommands() {
        Objects.requireNonNull(GardensFurniture.plugin.getCommand("nbt")).setExecutor(new NbtCommand());
        Objects.requireNonNull(GardensFurniture.plugin.getCommand("get")).setExecutor(new GetCommand());
        Objects.requireNonNull(GardensFurniture.plugin.getCommand("furniture")).setExecutor(new FurnitureCommand());
        Objects.requireNonNull(GardensFurniture.plugin.getCommand("itemsaver")).setExecutor(new ItemSaverCommand());

    }
}
