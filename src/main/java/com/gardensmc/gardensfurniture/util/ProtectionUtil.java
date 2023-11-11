package com.gardensmc.gardensfurniture.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import org.bukkit.Location;
import org.bukkit.entity.Player;


public class ProtectionUtil {

    // todo implement block place event to check
    public static boolean canBuild(Location location, Player player) {
        // ensure we have gardens players plugin, if so run isle perm check
//        if (GardensFurniture.gardensPlayers == null) {
//            return true;
//        }
        return true;
//        return ProtectionEvents.isTrusted(player.getWorld(), player);
    }
}
