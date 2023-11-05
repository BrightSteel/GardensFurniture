package com.gardensmc.gardensfurniture.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Rotation;
import org.bukkit.entity.Player;

public class PlayerUtil {

    public static void addPermission(Player player, String permission) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission set " + permission);
    }

    public static void sendSuccess(Player player, String message) {
        player.sendMessage( ChatColor.GREEN + message);
    }

    public static void sendError(Player player, String message) {
        player.sendMessage(ChatColor.RED + message);
    }
}
