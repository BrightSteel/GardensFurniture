package com.gardensmc.gardensfurniture.custom.type;

import org.bukkit.block.BlockFace;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RotationTest {

    // furniture faces back towards the player who placed it - the table the plugin always used
    @ParameterizedTest
    @CsvSource({
            "-135, 90", "-90, 90", "-45, 90",
            "-44, 180", "0, 180", "44, 180",
            "45, -90", "90, -90", "135, -90",
            "136, 0", "180, 0", "-136, 0", "-180, 0"
    })
    void fromPlayerFacing(float playerYaw, float displayYaw) {
        var rotation = Rotation.fromPlayerFacing(playerYaw);
        assertEquals(displayYaw, rotation.getYaw());
        assertEquals(0, rotation.getPitch());
    }

    @ParameterizedTest
    @CsvSource({
            "UP, 0, 0", "DOWN, 0, 0",
            "SOUTH, 180, -90", "NORTH, 0, -90", "WEST, -90, -90", "EAST, 90, -90"
    })
    void fromBlockFace(BlockFace face, float yaw, float pitch) {
        var rotation = Rotation.fromBlockFace(face);
        assertEquals(yaw, rotation.getYaw());
        assertEquals(pitch, rotation.getPitch());
    }
}
