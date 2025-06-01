package com.gardensmc.gardensfurniture.structure.structures;

import com.gardensmc.gardensfurniture.structure.TStructure;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.*;

public class WisteriaTree extends TStructure {

    private final Color color = Color.random();

    @Override
    public void generate() {
        var  randPrecision = 100;
        var  goldenAngle = 2.39996;
        var  pi = 3.14159;
        var  twopi = 6.28318530718;

        var  trunkHeight = 4+randomInt(2);
        var trunkBlock = "minecraft:cherry_log";

        var  sideBranchInterval = 5+randomInt(2);
        var  sideBranchMinHeight = 2+randomInt(3);
        var  sideBranchLengthMin = 3;
        var  sideBranchLengthVariation = 2;
        var  sideBranchVerticalVariation = 0;
        var  sideBranchVerticalDirection = 1+randomInt(randPrecision)/randPrecision/2;
        var  sideBranchLeafRadiusMin = 2;
        var  sideBranchLeafTranslate = 0;
        var  sideBranchLeafTopFlatness = 2;
        var  sideBranchLeafBottomFlatness = 4;
        var  sideBranchLeafDensity = 0.6;
        var sideBranchBlockX = "minecraft:cherry_log[axis=x]";
        var sideBranchBlockY = "minecraft:cherry_log[axis=y]";
        var sideBranchBlockZ = "minecraft:cherry_log[axis=z]";

        var  topBranches = 5+randomInt(2);
        var  topBranchLengthUpwardsMin = 4+randomInt(3);
        var  topBranchLengthOutwardsMin = 3+randomInt(3);
        var  topBranchVerticalDirection = 0.4;
        var  topBranchLeafRadiusMin = 2;
        var  topBranchLeafTranslate = 0;
        var  topBranchLeafTopFlatness = 1.5;
        var  topBranchLeafBottomFlatness = 1;
        var  topBranchLeafDensity = 0.6;
        var topBranchBlockX = "minecraft:cherry_log[axis=x]";
        var topBranchBlockY = "minecraft:cherry_log[axis=y]";
        var topBranchBlockZ = "minecraft:cherry_log[axis=z]";

        var  branchAngle = randomInt(randPrecision)/randPrecision*2*pi;
        for (var  i = 0; i < trunkHeight; i = i + 1) {
            if (i > sideBranchMinHeight) {
                if (i % sideBranchInterval == 0) {
                    // Branch direction vector
                    double  dx = sin(branchAngle);
                    double  dy = sideBranchVerticalDirection + (double) randomInt(randPrecision) /randPrecision*sideBranchVerticalVariation;
                    double  dz = cos(branchAngle);
                    // Normalize vector
                    double mag = sqrt(pow(dx,2)+pow(dy,2)+pow(dz,2));
                    dx = dx/mag;
                    dy = dy/mag;
                    dz = dz/mag;
                    // Branch origin
                    var  ox = 0;
                    var  oy = i;
                    var  oz = 0;
                    // Set branch block based on largest unsigned vector component
                    var branchBlock = sideBranchBlockY;
                    if (abs(dx) > max(abs(dy),abs(dz))) branchBlock = sideBranchBlockX;
                    else if (abs(dz) > max(abs(dy),abs(dx))) branchBlock = sideBranchBlockZ;
                    var  branchLength = sideBranchLengthMin + randomInt(randPrecision)/randPrecision*sideBranchLengthVariation;
                    // Generate branch
                    for (var  l = 1; l <= branchLength; l = l + 1) {
                        block(l*dx+ox, l*dy+oy, l*dz+oz, branchBlock);
                    }
                    // Rotate angle of next branch
                    branchAngle = branchAngle + goldenAngle;
                    // Generate leaf cluster at the end of the branch
                    var  radius = sideBranchLeafRadiusMin;
                    var  radiusSquared = pow(radius, 2);
                    // Leaf cluster origin (end of branch)
                    var  lox = ox+branchLength*dx;
                    var  loy = oy+branchLength*dy + sideBranchLeafTranslate;
                    var  loz = oz+branchLength*dz;
                    new WisteriaLeafClump(color).setOrigin(origin.clone().add(lox, loy, loz)).generate();
                }
            }
            block(0, i, 0, trunkBlock);
        }

        for (var  b = 1; b <= topBranches; b = b + 1) {
            var  t = b/topBranches;
            var  ti = 1-t;
            // Branch direction vector
            double  dx = sin(branchAngle) * t;
            double  dy = topBranchVerticalDirection;
            double  dz = cos(branchAngle) * t;
            // Normalize vector
            var  mag = sqrt(pow(dx,2)+pow(dy,2)+pow(dz,2));
            dx = dx/mag;
            dy = dy/mag;
            dz = dz/mag;
            // Branch origin
            var  ox = 0;
            var  oy = trunkHeight;
            var  oz = 0;
            // Set branch block based on largest unsigned vector component
            var branchBlock = topBranchBlockY;
            if (abs(dx) > max(abs(dy),abs(dz))) branchBlock = topBranchBlockX;
            else if (abs(dz) > max(abs(dy),abs(dx))) branchBlock = topBranchBlockZ;
            var  branchLength = topBranchLengthUpwardsMin*ti + topBranchLengthOutwardsMin*t;
            // Generate branch
            for (var  l = 0; l <= branchLength; l = l + 1) {
                block(l*dx+ox, l*dy+oy, l*dz+oz, branchBlock);
            }
            // Rotate angle of next branch
            branchAngle = branchAngle + goldenAngle;
            // Generate leaf cluster at the end of the branch
            var  radius = topBranchLeafRadiusMin;
            var  radiusSquared = pow(radius, 2);
            // Leaf cluster origin (end of branch)
            var  lox = ox+branchLength*dx;
            var  loy = oy+branchLength*dy + topBranchLeafTranslate;
            var  loz = oz+branchLength*dz;
            new WisteriaLeafClump(color).setOrigin(origin.clone().add(lox, loy, loz)).generate();
        }

        var  beeNestRoll = randomInt(30); // 1 in 30 chance
        if (beeNestRoll == 0) {
            new BeeNest().setOrigin(origin.clone().add(0, sideBranchMinHeight-1, 0)).generate();
        }

    }

    @AllArgsConstructor
    @Getter
    enum Color {
        PINK("wisteria_pink_leaves_half", "wisteria_pink_leaves", "wisteria_pink_vine"),
        PURPLE("wisteria_purple_leaves_half", "wisteria_purple_leaves", "wisteria_purple_vine"),
        WHITE("wisteria_white_leaves_half", "wisteria_white_leaves", "wisteria_white_vine");

        private final String leavesHalf;
        private final String leaves;
        private final String vine;

        public static Color random() {
            var colors = Arrays.stream(Color.values()).toList();
            return colors.get(ThreadLocalRandom.current().nextInt(colors.size()));
        }
    }
}
