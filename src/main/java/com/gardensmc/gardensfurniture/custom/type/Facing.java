package com.gardensmc.gardensfurniture.custom.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

/**
 * The four horizontal directions, and the one place that turns yaws, block faces and offsets into them.
 */
@Getter
@AllArgsConstructor
public enum Facing {
    NORTH(180),
    EAST(-90),
    SOUTH(0),
    WEST(90);

    // Minecraft yaw of something facing this way
    private final float yaw;

    public static Facing fromYaw(float yaw) {
        // normalise first: the old check treated yaw 135-225 (north) as east
        yaw = Location.normalizeYaw(yaw);
        if (yaw >= -135 && yaw <= -45) {
            return EAST;
        } else if (yaw > -45 && yaw < 45) {
            return SOUTH;
        } else if (yaw >= 45 && yaw <= 135) {
            return WEST;
        }
        return NORTH;
    }

    // throws for UP, DOWN and the diagonal faces
    public static Facing fromBlockFace(BlockFace face) {
        return valueOf(face.name());
    }

    public BlockFace toBlockFace() {
        return BlockFace.valueOf(name());
    }

    public Facing opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case EAST -> WEST;
            case SOUTH -> NORTH;
            case WEST -> EAST;
        };
    }

    public Facing clockwise() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
        };
    }

    // turn an offset written for something facing south (yaw 0) so it faces this way
    public Vector rotate(Vector vector) {
        return switch (this) {
            case SOUTH -> vector.clone();
            case WEST -> new Vector(-vector.getZ(), vector.getY(), vector.getX());
            case NORTH -> new Vector(-vector.getX(), vector.getY(), -vector.getZ());
            case EAST -> new Vector(vector.getZ(), vector.getY(), -vector.getX());
        };
    }
}
