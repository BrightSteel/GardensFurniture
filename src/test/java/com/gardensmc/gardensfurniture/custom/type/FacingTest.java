package com.gardensmc.gardensfurniture.custom.type;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class FacingTest {

    @ParameterizedTest
    @CsvSource({
            "0, SOUTH", "44.9, SOUTH", "-44.9, SOUTH",
            "45, WEST", "90, WEST", "135, WEST",
            "135.1, NORTH", "180, NORTH", "-180, NORTH", "-135.1, NORTH",
            "-135, EAST", "-90, EAST", "-45, EAST",
            // outside -180..180: normalised first (180 used to come out as EAST)
            "270, EAST", "-270, WEST", "360, SOUTH", "225, EAST", "540, NORTH"
    })
    void fromYaw(float yaw, Facing expected) {
        assertEquals(expected, Facing.fromYaw(yaw));
    }

    @ParameterizedTest
    @EnumSource(Facing.class)
    void yawRoundTrips(Facing facing) {
        assertEquals(facing, Facing.fromYaw(facing.getYaw()));
    }

    @ParameterizedTest
    @EnumSource(Facing.class)
    void blockFaceRoundTrips(Facing facing) {
        assertEquals(facing, Facing.fromBlockFace(facing.toBlockFace()));
        assertEquals(facing.name(), facing.toBlockFace().name());
    }

    @Test
    void fromBlockFaceRejectsNonCardinal() {
        assertThrows(IllegalArgumentException.class, () -> Facing.fromBlockFace(BlockFace.UP));
    }

    @ParameterizedTest
    @EnumSource(Facing.class)
    void oppositeAndClockwise(Facing facing) {
        assertEquals(facing, facing.opposite().opposite());
        assertEquals(facing.opposite(), facing.clockwise().clockwise());
        assertEquals(facing, facing.clockwise().clockwise().clockwise().clockwise());
        assertEquals(facing.toBlockFace().getOppositeFace(), facing.opposite().toBlockFace());
    }

    @Test
    void clockwiseOrder() {
        assertEquals(Facing.EAST, Facing.NORTH.clockwise());
        assertEquals(Facing.SOUTH, Facing.EAST.clockwise());
        assertEquals(Facing.WEST, Facing.SOUTH.clockwise());
        assertEquals(Facing.NORTH, Facing.WEST.clockwise());
    }

    // the exact mapping barrier offsets always used; placed furniture depends on it
    @Test
    void rotateOffsets() {
        var offset = new Vector(1, 3, 2);
        assertEquals(new Vector(1, 3, 2), Facing.SOUTH.rotate(offset));
        assertEquals(new Vector(-2, 3, 1), Facing.WEST.rotate(offset));
        assertEquals(new Vector(-1, 3, -2), Facing.NORTH.rotate(offset));
        assertEquals(new Vector(2, 3, -1), Facing.EAST.rotate(offset));
        assertEquals(new Vector(1, 3, 2), offset, "rotate must not change its input");
    }
}
