package com.gardensmc.gardensfurniture.command;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.loader.ChunkFurnitureLoader;
import com.gardensmc.gardensfurniture.menu.FurnitureMenu;
import com.gardensmc.gardensfurniture.permission.Permissions;
import com.gardensmc.gardensfurniture.structure.StructureHandler;
import com.gardensmc.gardensfurniture.util.CommandSenderUtil;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import com.gardensmc.gardensfurniture.util.PlayerUtil;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FurnitureCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (args.length > 0) {
            switch (args[0].toUpperCase()) {
                case "STRUCTURE" -> executeSpawnStructure(commandSender, args);
                case "RELOAD" -> executeReload(commandSender);
                case "GET" -> executeGet(commandSender, args);
                case "GIVE" -> executeGive(commandSender, args);
                case "INFO" -> executeInfo(commandSender);
                case "LOAD_SPAWN" -> loadSpawnFurniture(commandSender);
            }
        } else if (commandSender instanceof Player player) {
            new FurnitureMenu(player).open();
        } else {
            return false;
        }
        return true;
    }

    private void executeSpawnStructure(CommandSender commandSender, String[] arguments) {
        if (!commandSender.hasPermission(Permissions.SPAWN_STRUCTURE)) {
           CommandSenderUtil.sendPermissionError(commandSender);
        } else if (!(commandSender instanceof Player player)) {
            CommandSenderUtil.sendPlayerOnly(commandSender);
        } else if (arguments.length == 1) {
            PlayerUtil.sendError(player, "Please specify the name of the desired structure");
        } else {
            String structureName = arguments[1].toLowerCase();
            if (StructureHandler.INSTANCE.spawnStructure(structureName, player.getLocation())) {
                PlayerUtil.sendSuccess(player, "Spawned " + structureName + " successfully!");
            } else {
                PlayerUtil.sendError(player, "Failed to spawn " + structureName + ". Check console for details");
            }
        }
    }

    private void executeReload(CommandSender commandSender) {
        if (!commandSender.hasPermission(Permissions.RELOAD)) {
            CommandSenderUtil.sendPermissionError(commandSender);
        } else {
            ConfigUtil.loadConfigs();
            GardensFurniture.itemRegistry.reload();
            CommandSenderUtil.sendSuccess(commandSender, "Reloaded Gardens Furniture!");
        }
    }

    private void executeGet(CommandSender commandSender, String[] arguments) {
        if (!commandSender.hasPermission(Permissions.FURNITURE_GIVE)) {
            CommandSenderUtil.sendPermissionError(commandSender);
        } else if (!(commandSender instanceof Player player)) {
            CommandSenderUtil.sendPlayerOnly(commandSender);
        } else if (arguments.length < 2) {
            PlayerUtil.sendError(player, "Missing item name");
        } else {
            String itemName = arguments[1];
            ItemStack itemStack = GardensFurniture.itemRegistry.buildItem(itemName);
            if (itemStack == null) {
                PlayerUtil.sendError(player, "No item named " + itemName);
            } else {
                player.getInventory().addItem(itemStack);
                PlayerUtil.sendSuccess(player, "Received " + itemName);
            }
        }
    }

    private void executeGive(CommandSender commandSender, String[] arguments) {
        if (!commandSender.hasPermission(Permissions.FURNITURE_GIVE)) {
            CommandSenderUtil.sendPermissionError(commandSender);
        } else if (arguments.length < 3) {
            CommandSenderUtil.sendError(commandSender, "/furniture give <user> <itemName> [amount]");
        } else {
            String target = arguments[1];
            String itemName = arguments[2];
            Player player = Bukkit.getServer().getPlayer(target);
            if (player == null) {
                CommandSenderUtil.sendError(commandSender, "Unable to find user: " + target);
            } else {
                ItemStack itemStack = GardensFurniture.itemRegistry.buildItem(itemName);
                if (itemStack == null) {
                    CommandSenderUtil.sendError(commandSender, "No item named " + itemName);
                } else {
                    if (arguments.length > 3) {
                        try {
                            itemStack.setAmount(Integer.parseInt(arguments[3]));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            CommandSenderUtil.sendError(commandSender, "Unable to parse given amount: " + arguments[3] + ". Defaulting to 1 item");
                        }
                    }
                    player.getInventory().addItem(itemStack);
                    CommandSenderUtil.sendSuccess(commandSender, String.format("Gave %s %s %s", player.getName(), itemStack.getAmount(), itemName));
                }
            }
        }
    }

    private void executeInfo(CommandSender commandSender) {
        if (!commandSender.hasPermission(Permissions.FURNITURE_INFO)) {
            CommandSenderUtil.sendPermissionError(commandSender);
        } else if (!(commandSender instanceof Player player)) {
            CommandSenderUtil.sendPlayerOnly(commandSender);
        } else {
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            NBTItem nbtItem = new NBTItem(heldItem);
            player.sendMessage(nbtItem.toString());
        }
    }

    private void loadSpawnFurniture(CommandSender commandSender) {
        if (!commandSender.hasPermission(Permissions.LOAD_SPAWN_FURNITURE)) {
            CommandSenderUtil.sendPermissionError(commandSender);
        } else if (!(commandSender instanceof Player player)) {
            CommandSenderUtil.sendPlayerOnly(commandSender);
        } else {
            commandSender.sendMessage("Loading spawn furniture...");
            var centerChunk = player.getLocation().getChunk();
            int i = 1;
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    var chunk = centerChunk.getWorld().getChunkAt(centerChunk.getX() + x, centerChunk.getZ() + z);
                    var snapshot = chunk.getChunkSnapshot();
                    int finalI = i;
                    CompletableFuture
                            .runAsync(() -> ChunkFurnitureLoader.INSTANCE.processSnapshot(snapshot))
                            .thenRun(() -> player.sendMessage("Loaded chunk furniture " + finalI + "/25"));
                    i++;
                }
            }
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] arguments) {
        List<String> suggestions = new ArrayList<>();
        if (arguments.length > 0) {
            switch (arguments[0]) {
                case "GET" -> suggestions = GardensFurniture.itemRegistry.getItemIdentifiers();
            }
        }
        return suggestions;
    }
}
