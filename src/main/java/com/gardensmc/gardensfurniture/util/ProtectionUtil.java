package com.gardensmc.gardensfurniture.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ProtectionUtil {

    public static boolean canBuild(Location location, Player player) {
        if (!GardensFurniture.isGriefPreventionHooked) {
            return true;
        }
        return GriefPrevention.instance.allowBuild(player, location) == null;
    }
}
