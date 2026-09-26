package com.gardensmc.gardensfurniture.command;

import com.gardensmc.gardensfurniture.menu.DyeMenu;
import com.gardensmc.gardensfurniture.permission.Permissions;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DyeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if (!(commandSender instanceof Player player)) {
            return false;
        }
        if (!player.hasPermission(Permissions.PAINT)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Use a paint bench to dye items"));
            return true;
        }
        new DyeMenu(player).open();
        return true;
    }
}
