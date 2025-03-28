package com.gardensmc.gardensfurniture.util;

import org.bukkit.entity.Player;

public class DirectionUtil {

    public static String getDirectionFromFacing(Player player) {
        String rotation;
        float yaw = player.getLocation().getYaw();
        if (yaw >= -135 && yaw <= -45) {
            rotation = "EAST";
        }
        else if (yaw > -45 && yaw < 45) {
            rotation = "SOUTH";
        }
        else if (yaw >= 45 && yaw <= 135) {
            rotation = "WEST";
        }
        else {
            rotation = "NORTH";
        }
        return rotation;
    }
}
