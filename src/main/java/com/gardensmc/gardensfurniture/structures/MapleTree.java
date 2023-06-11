package com.gardensmc.gardensfurniture.structures;

import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.CustomItem;
import com.gardensmc.gardensfurniture.register.CustomRegister;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.pow;
import static java.lang.Math.round;

/**
 * Mirrors autumnal_birch_tree_procedural.tesf
 */
public class MapleTree extends TStructure {

    public MapleTree(Location location) {
        super(location);
    }

    @Override
    public void generate() {
        // Free to modify variables
        double leafRadius = 2.5 + ThreadLocalRandom.current().nextInt(4);
        int trunkHeight = 7 + ThreadLocalRandom.current().nextInt(5);
        double leafTopHeight = trunkHeight * 1.5;
        int leafBottomHeight = 3 + ThreadLocalRandom.current().nextInt(2);
        String trunkBlockX = "minecraft:note_block[instrument=basedrum,note=10]";
        String trunkBlockY = "minecraft:note_block[instrument=basedrum,note=9]";
        String trunkBlockZ = "minecraft:note_block[instrument=basedrum,note=11]";
        int leaf = ThreadLocalRandom.current().nextInt(4);
        String leafBlock = "maple_leaves_red";
        if (leaf == 1) {
            leafBlock = "maple_leaves_orange";
        }
        else if (leaf == 2) {
            leafBlock = "maple_leaves_gold";
        }
        else if (leaf == 3) {
            leafBlock = "maple_leaves_green";
        }

        // Tree gen logic
        double leafMidPointRatio = 0.4;
        double leafMidHeight = leafBottomHeight + (leafTopHeight-leafBottomHeight) * leafMidPointRatio;
        for (double y = 0; y <= leafTopHeight; y = y + 1) {
            double radius = 0;

            // Radius formula - Two parabolas that intersect at the variables above
            if (y < leafMidHeight) {
                radius = (-pow((y-leafMidHeight)/(leafBottomHeight-leafMidHeight),2)+1)*leafRadius;
            } else {
                radius = (-pow((y-leafMidHeight)/(leafTopHeight-leafMidHeight),2)+1)*leafRadius;
            }

            if (y >= leafBottomHeight) {
                if (radius <= 1) { // Skip generating circle if radius is less than 1
                    furniture(0, y, 0, leafBlock);

                } else {
                    // Generate circle of leaves at y level
                    double squareradius = radius * radius; // Square the radius to avoid unnecessary square root when comparing dist
                    for (double x = round(-radius); x <= round(radius); x = x + 1) {
                        for (double z = round(-radius); z <= round(radius); z = z + 1) {
                            double squaredist = x*x+z*z;
                            if (squaredist <= squareradius) {
                                if (squareradius - squaredist < squareradius * 0.5) { // If close to edge, make fuzzy
                                    if (ThreadLocalRandom.current().nextInt(2) == 0) {
                                        furniture(x, y, z, leafBlock);
                                    }
                                } else {
                                    furniture(x, y, z, leafBlock);
                                }
                            }
                        }
                    }
                }
            }

            if (y <= trunkHeight) {
                block(0, y, 0, trunkBlockY); // Main trunk
            }
        }

        int beeNestRoll = ThreadLocalRandom.current().nextInt(30); // 1 in 30 chance
        if (beeNestRoll == 0) {
            new BeeNest(origin.clone().add(0, leafBottomHeight, 0)).generate();
        }

        block(0, -1, 0, "minecraft:dirt");

    }

}
