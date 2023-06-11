package com.gardensmc.gardensfurniture.structures;

import org.bukkit.Location;

public class BeeNest extends TStructure{

    public BeeNest(Location origin) {
        super(origin);
    }

    @Override
    public void generate() {
        int dir = randomInt(4);
        int x = 0;
        int z = 0;
        String face = "west";

        if (dir == 0) {
            face = "west";
            x = -1;
            z = 0;
        } else if (dir == 1) {
            face = "north";
            x = 0;
            z = -1;
        } else if (dir == 2) {
            face = "east";
            x = 1;
            z = 0;
        } else if (dir == 3) {
            face = "south";
            x = 0;
            z = 1;
        }

        block(x, 0, z, "minecraft:bee_nest[facing=" + face + "]");
    }
}
