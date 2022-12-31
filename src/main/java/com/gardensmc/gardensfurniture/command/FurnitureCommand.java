package com.gardensmc.gardensfurniture.command;

import com.gardensmc.gardensfurniture.register.CustomRegister;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class FurnitureCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if (args.length > 0) {
            switch (args[0]) {
                case "reload" -> {
                    ConfigUtil.loadConfigs();
                    CustomRegister.INSTANCE.reload();
                    commandSender.sendMessage(ChatColor.GREEN + "Reloaded Gardens Furniture!");
                }
            }
        }

        return true;
    }
}
