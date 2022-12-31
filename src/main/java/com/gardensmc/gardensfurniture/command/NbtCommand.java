package com.gardensmc.gardensfurniture.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class NbtCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if (!(commandSender instanceof Player player)) return false;

        if (args.length < 1) return false;

        try {
            Integer custom_model = Integer.parseInt(args[0]);
            ItemStack stack = player.getInventory().getItemInMainHand();
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(custom_model);
                stack.setItemMeta(meta);
            }
            player.getInventory().setItemInMainHand(stack);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
