package com.gardensmc.gardensfurniture.command;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.wrap.WrapHandler;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class UnwrapCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if (!(commandSender instanceof Player player)) {
            return false;
        }
        var heldItem = player.getInventory().getItemInMainHand();
        var unwrappedResult = new WrapHandler(heldItem).getUnwrappedResult();
        if (unwrappedResult == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>This item is not wrapped!"));
        } else {
            player.getInventory().addItem(Objects.requireNonNull(GardensFurniture.itemRegistry.buildItem(unwrappedResult.wrapItem())));
            player.getInventory().setItemInMainHand(unwrappedResult.unwrappedItem());
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Unwrapped item"));
        }
        return true;
    }
}
