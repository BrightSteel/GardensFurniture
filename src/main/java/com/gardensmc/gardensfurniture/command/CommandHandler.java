package com.gardensmc.gardensfurniture.command;

import com.gardensmc.gardensfurniture.GardensFurniture;
import org.bukkit.command.CommandExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CommandHandler {

    private static final HashMap<String, CommandExecutor> commands = new HashMap<>();

    public static void registerCommands() {
        createCommands();
        for (Map.Entry<String, CommandExecutor> entry : commands.entrySet()) {
            Objects.requireNonNull(GardensFurniture.plugin.getCommand(entry.getKey())).setExecutor(entry.getValue());
        }
    }

    private static void createCommands() {
        commands.put("furniture", new FurnitureCommand());
        commands.put("paint", new DyeCommand());
    }
}
