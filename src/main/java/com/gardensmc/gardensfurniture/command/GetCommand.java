package com.gardensmc.gardensfurniture.command;

import com.gardensmc.gardensfurniture.register.CustomRegister;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GetCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if (!(commandSender.hasPermission("gardens.furniture.all"))) return false;

        if (!(commandSender instanceof Player player)) return false;

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Please enter the item identifier");
            return true;
        }

        String identifier = args[0];
        ItemStack itemStack = CustomRegister.INSTANCE.itemRegistry.buildItem(identifier);
        if (itemStack == null) {
            player.sendMessage(ChatColor.RED + "No item exists by that identifier!");
            return true;
        }

        player.getInventory().addItem(itemStack);
        player.sendMessage(ChatColor.GREEN + "Received " + identifier);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
        return CustomRegister.INSTANCE.itemRegistry.getItemIdentifiers();
    }
}
