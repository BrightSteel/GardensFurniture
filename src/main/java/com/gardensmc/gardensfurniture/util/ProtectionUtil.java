package com.gardensmc.gardensfurniture.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensplayers.event.ProtectionEvents;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class ProtectionUtil {

    public static boolean canBuild(Location location, Player player) {
        // ensure we have gardens players plugin, if so run isle perm check
        if (GardensFurniture.gardensPlayers == null) {
            return true;
        }
        return ProtectionEvents.isTrusted(player.getWorld(), player);
    }
}
