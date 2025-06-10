package com.gardensmc.gardensfurniture.structure.structures;

import com.gardensmc.gardensfurniture.structure.TStructure;

import static java.lang.Math.*;

public class PalmTree extends TStructure {

    @Override
    public void generate() {
        var randPrecision = 100;
        double goldenAngle = 2.39996;
        double pi = 3.14159;

        var height = 5 + randomInt(6);
        double x = 0;
        double y = 0;
        double z = 0;
        double dx = (double) (randomInt(11) - 5) /15;
        double dz = (double) (randomInt(11) - 5) /15;
        var leafBlock = "palm_leaves";

        // Generate trunk
        for (var unused = 0; y < height; y = y + 1) {
            furniture(x, y, z, "eucalyptus_log");
            furniture(x, y+1, z, "eucalyptus_log");
            x = x + dx;
            z = z + dz;
        }

        // Generate leaves at top
        double branches = 10;
        double branchLength = 3+ (double) height /4;
        double droop = 0.03;

        double branchAngle = (double) randomInt(randPrecision) /randPrecision*2*pi;
        double dy = 0;
        double mag = 1;
        for (var b = 0; b < branches; b = b + 1) {
            // Branch direction vector
            dx = sin(branchAngle);
            dy = b/branches*2-1;
            dz = cos(branchAngle);
            // Normalize vector
            mag = sqrt(pow(dx,2)+pow(dy,2)+pow(dz,2));
            dx = dx/mag;
            dy = dy/mag;
            dz = dz/mag;
            for (double i = 0; i < branchLength; i = i + 0.5) {
                furniture(x+dx*i, y+dy*i, z+dz*i, leafBlock);
                furniture(x+dx*i, y+dy*i+0.5, z+dz*i, leafBlock);
                // Downwards velocity as branch is further away
                dy = dy - droop;
                // Normalize vector
                mag = sqrt(pow2(dx)+pow2(dy)+pow2(dz));
                dx = dx/mag;
                dy = dy/mag;
                dz = dz/mag;
            }
            branchAngle = branchAngle + goldenAngle;
        }

    }
}
