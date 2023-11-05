package com.gardensmc.gardensfurniture.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandSenderUtil {

    public static void sendPlayerOnly(CommandSender commandSender) {
        commandSender.sendMessage(ChatColor.RED + "Only players can run this command!");
    }

    public static void sendPermissionError(CommandSender commandSender) {
        sendError(commandSender, "You do not have permission to perform this command!");
    }

    public static void sendError(CommandSender commandSender, String message) {
        commandSender.sendMessage(ChatColor.RED + message);
    }

    public static void sendSuccess(CommandSender commandSender, String message) {
        commandSender.sendMessage(ChatColor.GREEN + message);
    }
}
