package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.custom.type.Rotation;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// turning a placed piece re-rotates its configured barrier offsets to the new yaw (see CustomDisplayBlock#rotate)
class BarrierRotationTest {

    private static final List<Vector> SOFA = List.of(new Vector(0, 0, 0), new Vector(1, 0, 0));

    private static List<Vector> at(float yaw) {
        return SOFA.stream().map(v -> Barriers.rotate(v, new Rotation(yaw, 0))).toList();
    }

    @Test
    void fourQuarterTurnsComeBackToTheStart() {
        float yaw = 0;
        var start = at(yaw);
        for (int i = 0; i < 4; i++) yaw += 90;
        assertEquals(start, at(yaw % 360));
    }

    @Test
    void aQuarterTurnSwingsTheSecondCellSideways() {
        var facingSouth = at(0);
        var turned = at(90);
        assertNotEquals(facingSouth, turned);
        // the anchor cell never moves, and the piece stays two cells, one block apart
        assertEquals(new Vector(0, 0, 0), turned.get(0));
        assertEquals(1.0, turned.get(1).length(), 1e-9);
        assertEquals(0.0, turned.get(1).getY(), 1e-9);
    }

    @Test
    void storedOffsetsReadBackTheSame() {
        for (float yaw : new float[]{0, 90, 180, -90}) {
            for (Vector v : at(yaw)) {
                assertEquals(v, Barriers.parse(v.toString()));
            }
        }
    }
}
