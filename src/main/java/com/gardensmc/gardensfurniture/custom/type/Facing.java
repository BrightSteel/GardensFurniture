package com.gardensmc.gardensfurniture.custom.type;

public enum Facing {
    NORTH,
    EAST,
    SOUTH,
    WEST;

    public static Facing fromYaw(float yaw) {
        if ((yaw >= -135 && yaw <= -45) || (yaw > 135 && yaw < 315)) {
            return EAST;
        }
        else if (yaw > -45 && yaw < 45) {
            return SOUTH;
        }
        else if (yaw >= 45 && yaw <= 135) {
            return WEST;
        }
        return NORTH;
    }
}
