package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.custom.type.Facing;
import com.gardensmc.gardensfurniture.custom.type.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BarriersTest {

    @Test
    void parsesConfigFormat() {
        assertEquals(new Vector(1, 0, -1), Barriers.parse("1,0,-1"));
        assertEquals(new Vector(0, 2, 0), Barriers.parse("0,2,0"));
    }

    // barriers are stored on placed displays via Vector#toString and read back with parse:
    // existing furniture breaks if either side changes format
    @Test
    void readsWhatIsStored() {
        var stored = new Vector(-1, 2, 3);
        assertEquals(stored, Barriers.parse(stored.toString()));
    }

    @ParameterizedTest
    @EnumSource(Facing.class)
    void rotatesWithTheDisplay(Facing facing) {
        var offset = new Vector(1, 0, 2);
        var displayRotation = new Rotation(facing.getYaw(), 0);
        assertEquals(facing.rotate(offset), Barriers.rotate(offset, displayRotation));
    }

    // furniture that faces the clicked face (not the player) uses the block face's rotation
    @Test
    void floorPlacementIsNotRotated() {
        var offset = new Vector(1, 0, 2);
        assertEquals(offset, Barriers.rotate(offset, Rotation.fromBlockFace(BlockFace.UP)));
    }
}
