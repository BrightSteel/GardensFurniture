package com.gardensmc.gardensfurniture.command;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.dye.DyeColorGroups;
import com.gardensmc.gardensfurniture.loader.ChunkFurnitureLoader;
import com.gardensmc.gardensfurniture.menu.FurnitureMenu;
import com.gardensmc.gardensfurniture.permission.Permissions;
import com.gardensmc.gardensfurniture.recipe.RecipeManager;
import com.gardensmc.gardensfurniture.structure.StructureHandler;
import com.gardensmc.gardensfurniture.util.CommandSenderUtil;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import com.gardensmc.gardensfurniture.util.FurnitureCleanup;
import com.gardensmc.gardensfurniture.util.PlayerUtil;
import de.tr7zw.nbtapi.NBTItem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
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
        if (!commandSender.hasPermission("gardensfurniture.furniture")) {
            return false;
        }
        if (args.length > 0) {
            switch (args[0].toUpperCase()) {
                case "STRUCTURE" -> executeSpawnStructure(commandSender, args);
                case "RELOAD" -> executeReload(commandSender);
                case "GET" -> executeGet(commandSender, args);
                case "GIVE" -> executeGive(commandSender, args);
                case "INFO" -> executeInfo(commandSender);
                case "LOAD_SPAWN" -> loadSpawnFurniture(commandSender);
                case "CLEANUP" -> executeCleanup(commandSender, args);
                case "RECIPE", "RECIPES" -> RecipeCommand.execute(commandSender, args);
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
            GardensFurniture.dyeColorGroups = new DyeColorGroups(ConfigUtil.dyeColors);
            GardensFurniture.itemRegistry.reload();
            RecipeManager.INSTANCE.reload();
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
            int amount = 1;
            if (arguments.length > 2) {
                try {
                    amount = Integer.parseInt(arguments[2]);
                } catch (NumberFormatException e) {
                    amount = 0;
                }
            }
            if (itemStack == null) {
                PlayerUtil.sendError(player, "No item named " + itemName);
            } else if (amount < 1 || amount > 2304) {
                PlayerUtil.sendError(player, "Amount must be 1-2304 (a full inventory of 64s)");
            } else {
                itemStack.setAmount(amount);
                // addItem splits it into stacks the item allows; what doesn't fit lands at your feet
                player.getInventory().addItem(itemStack).values()
                        .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                PlayerUtil.sendSuccess(player, "Received " + amount + " " + itemName);
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

    /**
     * /furniture cleanup [radius] [confirm] - what broken furniture left behind within `radius` blocks
     * (default 32) of you. Reports only, unless "confirm" is given; see FurnitureCleanup for why.
     */
    private void executeCleanup(CommandSender commandSender, String[] args) {
        if (!commandSender.hasPermission(Permissions.CLEANUP)) {
            CommandSenderUtil.sendPermissionError(commandSender);
            return;
        }
        if (!(commandSender instanceof Player player)) {
            CommandSenderUtil.sendPlayerOnly(commandSender);
            return;
        }
        int radius = 32;
        if (args.length > 1) {
            try {
                radius = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                radius = -1;
            }
        }
        if (radius < 1 || radius > FurnitureCleanup.MAX_RADIUS) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Radius must be 1-" + FurnitureCleanup.MAX_RADIUS));
            return;
        }
        boolean confirm = args.length > 2 && args[2].equalsIgnoreCase("confirm");
        int finalRadius = radius;
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Scanning " + radius + " blocks around you…"));
        FurnitureCleanup.scan(player.getLocation(), radius).thenAccept(report -> {
            if (confirm) {
                int removed = FurnitureCleanup.remove(report);
                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Removed " + removed + " leftover(s)."));
                if (!report.unknownItems().isEmpty()) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>" + report.unknownItems().size()
                            + " furniture display(s) have items no longer in config - left alone."));
                }
                return;
            }
            player.sendMessage(MiniMessage.miniMessage().deserialize("<gold>Furniture leftovers within " + finalRadius + " blocks:"));
            line(player, "Stores for blocks that are gone", report.stores().stream().map(FurnitureCommand::at).toList());
            line(player, "Barriers no furniture uses", report.barriers().stream().map(FurnitureCommand::at).toList());
            line(player, "Light blocks no furniture lights", report.lights().stream().map(FurnitureCommand::at).toList());
            line(player, "Hitboxes with no furniture", report.hitboxes().stream().map(e -> at(e.getLocation().getBlock())).toList());
            line(player, "Displays whose item isn't in config (not removed)", report.unknownItems().stream().map(e -> at(e.getLocation().getBlock())).toList());
            if (report.removable() > 0) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Check these aren't barriers or lights you placed on purpose, then run <white>/furniture cleanup "
                        + finalRadius + " confirm</white> to remove them."));
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Nothing to clean up."));
            }
        });
    }

    private static String at(org.bukkit.block.Block block) {
        return block.getX() + " " + block.getY() + " " + block.getZ();
    }

    // "Barriers no furniture uses: 3 (12 64 -40, 13 64 -40, …)"
    private static void line(Player player, String label, List<String> spots) {
        String sample = spots.isEmpty() ? "" : " <dark_gray>(" + String.join(", ", spots.subList(0, Math.min(3, spots.size())))
                + (spots.size() > 3 ? ", …" : "") + ")";
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>" + label + ": <white>" + spots.size() + sample));
    }

    private void loadSpawnFurniture(CommandSender commandSender) {
        if (!commandSender.hasPermission(Permissions.LOAD_SPAWN_FURNITURE)) {
            CommandSenderUtil.sendPermissionError(commandSender);
        } else if (!(commandSender instanceof Player player)) {
            CommandSenderUtil.sendPlayerOnly(commandSender);
        } else {
            commandSender.sendMessage("Loading spawn furniture...");
            var centerChunk = player.getWorld().getSpawnLocation().getChunk();
            var radius = 2;
            int i = 1;
            var area = Math.pow(radius*2 + 1, 2);
            int minY = centerChunk.getWorld().getMinHeight(), maxY = centerChunk.getWorld().getMaxHeight();
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    var chunk = centerChunk.getWorld().getChunkAt(centerChunk.getX() + x, centerChunk.getZ() + z);
                    var snapshot = chunk.getChunkSnapshot();
                    int finalI = i;
                    CompletableFuture
                            .runAsync(() -> ChunkFurnitureLoader.INSTANCE.processSnapshot(snapshot, minY, maxY))
                            .thenRun(() -> player.sendMessage("Loaded chunk furniture " + finalI + "/" + area));
                    i++;
                }
            }
        }
    }

    // subcommand -> permission needed to see it suggested
    private static final java.util.Map<String, String> SUBCOMMANDS = new java.util.LinkedHashMap<>();

    static {
        SUBCOMMANDS.put("get", Permissions.FURNITURE_GIVE);
        SUBCOMMANDS.put("give", Permissions.FURNITURE_GIVE);
        SUBCOMMANDS.put("info", Permissions.FURNITURE_INFO);
        SUBCOMMANDS.put("recipe", Permissions.RECIPES);
        SUBCOMMANDS.put("reload", Permissions.RELOAD);
        SUBCOMMANDS.put("cleanup", Permissions.CLEANUP);
        SUBCOMMANDS.put("structure", Permissions.SPAWN_STRUCTURE);
        SUBCOMMANDS.put("load_spawn", Permissions.LOAD_SPAWN_FURNITURE);
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] arguments) {
        if (!commandSender.hasPermission("gardensfurniture.furniture") || arguments.length == 0) {
            return List.of();
        }
        if (arguments.length == 1) {
            return matching(SUBCOMMANDS.entrySet().stream()
                    .filter(e -> commandSender.hasPermission(e.getValue()))
                    .map(java.util.Map.Entry::getKey).toList(), arguments[0]);
        }
        var sub = arguments[0].toLowerCase();
        var permission = SUBCOMMANDS.get(sub.equals("recipes") ? "recipe" : sub);
        if (permission == null || !commandSender.hasPermission(permission)) {
            return List.of();
        }
        var last = arguments[arguments.length - 1];
        List<String> options = switch (sub) {
            case "get" -> arguments.length == 2 ? itemIds() : arguments.length == 3 ? List.of("1", "16", "64") : List.of();
            case "give" -> switch (arguments.length) {
                case 2 -> Bukkit.getOnlinePlayers().stream().map(org.bukkit.entity.Player::getName).toList();
                case 3 -> itemIds();
                case 4 -> List.of("1", "16", "64");
                default -> List.of();
            };
            case "structure" -> arguments.length == 2 ? StructureHandler.INSTANCE.getStructureNames().stream().sorted().toList() : List.of();
            case "cleanup" -> arguments.length == 2 ? List.of("16", "32", "64") : arguments.length == 3 ? List.of("confirm") : List.of();
            case "recipe", "recipes" -> RecipeCommand.tabComplete(arguments);
            default -> List.of();
        };
        return matching(options, last);
    }

    private static List<String> itemIds() {
        return GardensFurniture.itemRegistry.getItemIdentifiers().stream().sorted().toList();
    }

    // only what starts with what's typed so far; ids also match on any _-separated word ("planks" finds maple_planks)
    private static List<String> matching(List<String> options, String typed) {
        var prefix = typed.toLowerCase();
        return options.stream()
                .filter(o -> o.toLowerCase().startsWith(prefix) || o.toLowerCase().contains("_" + prefix))
                .toList();
    }
}
