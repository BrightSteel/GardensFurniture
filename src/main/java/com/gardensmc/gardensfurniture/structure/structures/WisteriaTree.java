package com.gardensmc.gardensfurniture.structure.structures;

import com.gardensmc.gardensfurniture.structure.TStructure;
import it.unimi.dsi.fastutil.Pair;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.block.BlockFace;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.*;

public class WisteriaTree extends TStructure {

    @Override
    public void generate() {
        Color color = Color.random();
        var  randPrecision = 100;
        var  goldenAngle = 2.39996;
        var  pi = 3.14159;
        var  twopi = 6.28318530718;

        var  trunkHeight = 4+randomInt(2);
        var trunkBlock = "wisteria_log";

        var  sideBranchInterval = 5+randomInt(2);
        var  sideBranchMinHeight = 2+randomInt(3);
        var  sideBranchLengthMin = 3;
        var  sideBranchLengthVariation = 2;
        var  sideBranchVerticalVariation = 0;
        double  sideBranchVerticalDirection = 1+ (double) randomInt(randPrecision) /randPrecision/2;
        var  sideBranchLeafRadiusMin = 2;
        var  sideBranchLeafTranslate = 0;
        var  sideBranchLeafTopFlatness = 2;
        var  sideBranchLeafBottomFlatness = 4;
        var  sideBranchLeafDensity = 0.6;
        var sideBranchBlockX = Pair.of("wisteria_log", BlockFace.EAST);
        var sideBranchBlockY = Pair.of("wisteria_log", BlockFace.UP);
        var sideBranchBlockZ = Pair.of("wisteria_log", BlockFace.NORTH);

        var  topBranches = 5+randomInt(2);
        var  topBranchLengthUpwardsMin = 4+randomInt(3);
        var  topBranchLengthOutwardsMin = 3+randomInt(3);
        var  topBranchVerticalDirection = 0.4;
        var  topBranchLeafRadiusMin = 2;
        var  topBranchLeafTranslate = 0;
        var  topBranchLeafTopFlatness = 1.5;
        var  topBranchLeafBottomFlatness = 1;
        var  topBranchLeafDensity = 0.6;
        var topBranchBlockX = Pair.of("wisteria_log", BlockFace.EAST);
        var topBranchBlockY = Pair.of("wisteria_log", BlockFace.UP);
        var topBranchBlockZ = Pair.of("wisteria_log", BlockFace.NORTH);

        var  branchAngle = (double) randomInt(randPrecision)/randPrecision*2*pi; // integer division made this always 0
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
                        furniture(l*dx+ox, l*dy+oy, l*dz+oz, branchBlock.left(), branchBlock.right());
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
            furniture(0, i, 0, trunkBlock);
        }

        for (var  b = 1; b <= topBranches; b = b + 1) {
            double  t = (double) b /topBranches;
            double  ti = 1-t;
            // Branch direction vector
            double  dx = sin(branchAngle) * t;
            double  dy = topBranchVerticalDirection;
            double  dz = cos(branchAngle) * t;
            // Normalize vector
            double  mag = sqrt(pow(dx,2)+pow(dy,2)+pow(dz,2));
            dx = dx/mag;
            dy = dy/mag;
            dz = dz/mag;
            // Branch origin
            double  ox = 0;
            double  oy = trunkHeight;
            double  oz = 0;
            // Set branch block based on largest unsigned vector component
            var branchBlock = topBranchBlockY;
            if (abs(dx) > max(abs(dy),abs(dz))) branchBlock = topBranchBlockX;
            else if (abs(dz) > max(abs(dy),abs(dx))) branchBlock = topBranchBlockZ;
            var  branchLength = topBranchLengthUpwardsMin*ti + topBranchLengthOutwardsMin*t;
            // Generate branch
            for (var  l = 0; l <= branchLength; l = l + 1) {
                furniture(l*dx+ox, l*dy+oy, l*dz+oz, branchBlock.left(), branchBlock.right());
            }
            // Rotate angle of next branch
            branchAngle = branchAngle + goldenAngle;
            // Generate leaf cluster at the end of the branch
            double  radius = topBranchLeafRadiusMin;
            var  radiusSquared = pow(radius, 2);
            // Leaf cluster origin (end of branch)
            double  lox = ox+branchLength*dx;
            double  loy = oy+branchLength*dy + topBranchLeafTranslate;
            double  loz = oz+branchLength*dz;
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
        PINK("wisteria_leaves_half_pink", "wisteria_leaves_pink", "wisteria_pink_vine"),
        PURPLE("wisteria_leaves_half_purple", "wisteria_leaves_purple", "wisteria_purple_vine"),
        WHITE("wisteria_leaves_half_white", "wisteria_leaves_white", "wisteria_white_vine"),
        BLUE("wisteria_leaves_half_blue", "wisteria_leaves_blue", "wisteria_blue_vine");

        private final String leavesHalf;
        private final String leaves;
        private final String vine;

        public static Color random() {
            var colors = Arrays.stream(Color.values()).toList();
            return colors.get(ThreadLocalRandom.current().nextInt(colors.size()));
        }
    }
}
