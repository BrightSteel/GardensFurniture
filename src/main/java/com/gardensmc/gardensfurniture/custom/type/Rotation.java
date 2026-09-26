package com.gardensmc.gardensfurniture.custom.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.block.BlockFace;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Rotation {

    private float yaw;
    private float pitch;

    public Rotation withYaw(float yaw) {
        this.yaw = yaw;
        return this;
    }

    public Rotation withPitch(float pitch) {
        this.pitch = pitch;
        return this;
    }

    public static Rotation fromBlockFace(BlockFace facing) {
        return switch (facing) {
            case UP, DOWN -> new Rotation();
            case SOUTH -> new Rotation(180, -90);
            case NORTH -> new Rotation(0, -90);
            case WEST -> new Rotation(-90, -90);
            case EAST -> new Rotation(90, -90);
            default -> throw new IllegalArgumentException("Block face must be a cardinal direction!");
        };
    }

    // furniture placed by a player faces back towards them
    public static Rotation fromPlayerFacing(float playerYaw) {
        return new Rotation(Facing.fromYaw(playerYaw).opposite().getYaw(), 0);
    }

}
