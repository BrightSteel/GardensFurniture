package com.gardensmc.gardensfurniture.command;

import com.gardensmc.gardensfurniture.register.CustomRegister;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class FurnitureCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if (!(commandSender.hasPermission("gardens.furniture.all"))) return false;

        if (args.length > 0) {
            switch (args[0]) {
                case "reload" -> {
                    ConfigUtil.loadConfigs();
                    CustomRegister.INSTANCE.reload();
                    commandSender.sendMessage(ChatColor.GREEN + "Reloaded Gardens Furniture!");
                }
                case "get" -> {
                    if (!(commandSender instanceof Player player)) {
                        return false;
                    }

                    if (!(commandSender.hasPermission("gardens.furniture.all"))) return false;
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.RED + "Please enter the item identifier");
                        return true;
                    }

                    String identifier = args[1];
                    ItemStack itemStack = CustomRegister.INSTANCE.itemRegistry.buildItem(identifier);
                    if (itemStack == null) {
                        player.sendMessage(ChatColor.RED + "No item exists by that identifier!");
                        return true;
                    }

                    player.getInventory().addItem(itemStack);
                    player.sendMessage(ChatColor.GREEN + "Received " + identifier);
                }
                case "give" -> {
                   // no feedback, used by console only right now
                    if (args.length < 3) {
                        return false;
                    }
                    String target = args[1];
                    String identifier = args[2];
                    Player player = Bukkit.getServer().getPlayer(target);
                    if (player != null) {
                        ItemStack itemStack = CustomRegister.INSTANCE.itemRegistry.buildItem(identifier);
                        itemStack.getItemMeta().addAttributeModifier(A)
                        if (itemStack != null) {
                            if (args.length > 3) {
                                try {
                                    int amount = Integer.parseInt(args[3]);
                                    itemStack.setAmount(amount);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                            player.getInventory().addItem(itemStack);
                        }
                    }
                }
                case "info" -> {
                    if (!(commandSender instanceof Player player)) {
                        return false;
                    }

                    ItemStack heldItem = player.getInventory().getItemInMainHand();
                    NBTItem nbtItem = new NBTItem(heldItem);
                    player.sendMessage(nbtItem.toString());
                }
            }
        }

        return true;
    }
}
