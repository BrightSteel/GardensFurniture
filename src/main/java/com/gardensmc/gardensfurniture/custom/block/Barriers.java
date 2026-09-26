package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.custom.type.Rotation;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Barrier offsets of multi-block display furniture: parsed from config ("x,y,z"), rotated to the
 * furniture's facing, and stored on the display entity so breaking can find every cell again.
 */
public final class Barriers {

    private static final String NBT_KEY = "FurnitureBarriers";

    private Barriers() {
    }

    // "x,y,z" - the config format, and also what Vector#toString writes to the entity
    public static Vector parse(String str) {
        var nums = str.split(",");
        return new Vector(Double.parseDouble(nums[0]), Double.parseDouble(nums[1]), Double.parseDouble(nums[2]));
    }

    // offsets are written facing north; turn them to face the same way as the furniture
    public static Vector rotate(Vector vector, float yaw) {
        return switch ((int) Rotation.fromPlayerFacing(yaw).getYaw()) {
            case 90 -> new Vector(-vector.getZ(), vector.getY(), vector.getX());
            case 180 -> new Vector(-vector.getX(), vector.getY(), -vector.getZ());
            case -90 -> new Vector(vector.getZ(), vector.getY(), -vector.getX());
            default -> vector.clone();
        };
    }

    public static void write(Entity display, List<Vector> barriers) {
        if (barriers.isEmpty()) return;
        NBT.modifyPersistentData(display, nbt -> {
            var stringList = nbt.getStringList(NBT_KEY);
            barriers.forEach(barrier -> stringList.add(barrier.toString()));
        });
    }

    public static List<Vector> read(Entity display) {
        List<String> stored = new ArrayList<>();
        NBT.getPersistentData(display, nbt -> {
            if (nbt.hasTag(NBT_KEY)) {
                nbt.getStringList(NBT_KEY).forEach(stored::add);
            }
            return null;
        });
        return stored.stream().map(Barriers::parse).toList();
    }
}
