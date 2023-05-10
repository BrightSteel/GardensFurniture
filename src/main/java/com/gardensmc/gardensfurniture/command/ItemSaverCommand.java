package com.gardensmc.gardensfurniture.command;

import com.gardensmc.gardensfurniture.util.ConfigUtil;
import com.gardensmc.gardensplayers.redis.RedisSender;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemSaverCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if (!commandSender.hasPermission("itemsaver.all")) return false;

        if (args.length > 1) {
            String arg = args[0];
            String name = args[1].toLowerCase();
            int amount = 0;
            try {
                if (args.length > 2) {
                    amount = Integer.parseInt(args[2]);
                }
            } catch (Exception ex) {
                commandSender.sendMessage("Amount must be a whole number");
            }
            switch (arg.toLowerCase()) {
                case "save" -> {
                    if (!(commandSender instanceof Player player)) return false;
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    if (mainHand.getType() == Material.AIR) {
                        return false;
                    }

                    ConfigurationSection section = ConfigUtil.itemSaverYaml;
                    section.set(name, mainHand);
                    ConfigUtil.saveItemSaverYaml();
                    player.sendMessage(ChatColor.GREEN + "Saved item as " + name + " successfully!");
                }
                case "get" -> {
                    if (!(commandSender instanceof Player player)) return false;
                    giveItem(name, player, amount);
                }
                case "give" -> {
                    if (args.length > 3) {
                        String player_name = args[3];
                        Player player = Bukkit.getPlayer(player_name);
                        if (player == null) {
                            // attempt give over proxy
                            String sender = "console";
                            if (commandSender instanceof Player sender1) {
                                sender = sender1.getUniqueId().toString();
                            }

                            RedisSender.sendGiveSavedItem(name, player_name, amount, sender);
                            commandSender.sendMessage(ChatColor.GREEN + "Attempting to give " + name + " over network...");
                        }
                        else {
                            giveItem(name, player, amount);
                        }
                    }
                }
                case "reload" -> {
                    ConfigUtil.reloadSavedItems();
                    commandSender.sendMessage(ChatColor.GREEN + "Reloaded config successfully!");
                }
            }
        }

        return true;
    }

    public static void giveItem(String name, Player player, int amount) {
        ConfigurationSection section = ConfigUtil.itemSaverYaml;
        ItemStack stack = section.getItemStack(name);
        if (stack == null) {
            player.sendMessage(ChatColor.RED + "No item saved by that name!");
        }
        else {
            if (amount > 1) {
                stack.setAmount(amount);
            }
            else {
                amount = 1;
            }
            player.getInventory().addItem(stack);
            player.sendMessage(ChatColor.GREEN + "Received " + amount + "x " + name);
        }
    }
}
