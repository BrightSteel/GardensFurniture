package com.gardensmc.gardensfurniture.util;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Rotation;
import org.bukkit.entity.Player;

public class ItemFrameUtil {

    public static Rotation getRotationFromFacing(Player player) {
        Rotation rotation;
        float yaw = player.getLocation().getYaw();
        if (yaw >= -135 && yaw <= -45) {
            rotation = Rotation.CLOCKWISE;
        }
        else if (yaw > -45 && yaw < 45) {
            rotation = Rotation.FLIPPED;
        }
        else if (yaw >= 45 && yaw <= 135) {
            rotation = Rotation.COUNTER_CLOCKWISE;
        }
        else {
            rotation = Rotation.NONE;
        }
        return rotation;
    }

    public static Pair<Float, Float> getDisplayRotationFromFacing(Player player) {
        float displayYaw;
        float yaw = player.getLocation().getYaw();
        if (yaw >= -135 && yaw <= -45) {
            displayYaw = 90;
        }
        else if (yaw > -45 && yaw < 45) {
            displayYaw = 180;
        }
        else if (yaw >= 45 && yaw <= 135) {
            displayYaw = -90;
        }
        else {
            displayYaw = 0;
        }
        return Pair.of(displayYaw, 0f);
    }
}
