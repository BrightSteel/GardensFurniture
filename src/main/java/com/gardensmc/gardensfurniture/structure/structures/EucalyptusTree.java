package com.gardensmc.gardensfurniture.structure.structures;

import com.gardensmc.gardensfurniture.structure.TStructure;

import static java.lang.Math.*;

public class EucalyptusTree extends TStructure {
    
    @Override
    public void generate() {
        var randPrecision = 100;
        var goldenAngle = 2.39996;
        var pi = 3.14159;

        var trunkHeight = 9+randomInt(7);
        var rootBlock = "minecraft:acacia_wood";

        double sideBranchInterval = 0;
        var sideBranchMinHeight = 3+randomInt(3);
        var sideBranchLengthMin = 4+randomInt(2);
        var sideBranchLengthVariation = 2;
        var sideBranchVerticalVariation = 0;
        var sideBranchVerticalDirection = 0.5+randomInt(randPrecision)/randPrecision/2;
        var sideBranchLeafRadiusMin = 3;
        var sideBranchLeafTranslate = 0;
        var sideBranchLeafTopFlatness = 0.5;
        var sideBranchLeafBottomFlatness = 2;
        var sideBranchLeafDensity = 0.7;
        var sideBranchBlockX = "minecraft:note_block[instrument=custom_head,note=19]";
        var sideBranchBlockY = "minecraft:note_block[instrument=custom_head,note=20]";
        var sideBranchBlockZ = "minecraft:note_block[instrument=custom_head,note=21]";

        var topBranches = 1;
        var topBranchLengthUpwardsMin = 7+randomInt(3);
        var topBranchLengthOutwardsMin = 0;
        var topBranchVerticalDirection = 1;
        var topBranchLeafRadiusMin = 3;
        var topBranchLeafTranslate = 0;
        var topBranchLeafTopFlatness = 0.7;
        var topBranchLeafBottomFlatness = 1.5;
        var topBranchLeafDensity = 0.7;
        var topBranchBlockX = "minecraft:note_block[instrument=custom_head,note=19]";
        var topBranchBlockY = "minecraft:note_block[instrument=custom_head,note=20]";
        var topBranchBlockZ = "minecraft:note_block[instrument=custom_head,note=21]";

        // Generate roots at base of trunk
//        var roots = 0;
//        var rootMaxDepth = 3;
//        var rootHeight = 5;
//        var rootLength = 2;
//        var rootAngle = randomInt(randPrecision)/randPrecision*2*pi;
//        for (var r = 0; r < roots; r = r + 1) {
//            var dx = sin(rootAngle);
//            var dz = cos(rootAngle);
//            for (var l = 0; l <= rootLength; l = l + 1) {
//                var height = rootHeight * (1-(l/rootLength));
//                var bottomedOut = false;
//                for (var h = height; h >= -rootMaxDepth; h = h - 1) {
//                    if (check(round(l*dx),round(h),round(l*dz)) != "AIR") {
//                        bottomedOut = true;
//                        break;
//                    }
//                    block(l*dx, h, l*dz, rootBlock);
//                }
//                if (!bottomedOut) break;
//            }
//            rootAngle = rootAngle + goldenAngle;
//        }

        double branchAngle = (double) randomInt(randPrecision) /randPrecision*2*pi;
        for (var i = 0; i < trunkHeight; i = i + 1) {
            if (i > sideBranchMinHeight) {
                if (i % sideBranchInterval == 0) {
                    // Branch direction vector
                    double dx = sin(branchAngle);
                    double dy = sideBranchVerticalDirection + (double) randomInt(randPrecision) /randPrecision*sideBranchVerticalVariation;
                    double dz = cos(branchAngle);
                    // Normalize vector
                    var mag = sqrt(pow(dx,2)+pow(dy,2)+pow(dz,2));
                    dx = dx/mag;
                    dy = dy/mag;
                    dz = dz/mag;
                    // Branch origin
                    var ox = 0;
                    var oy = i;
                    var oz = 0;
                    // Set branch block based on largest unsigned vector component
                    var branchBlock = sideBranchBlockY;
                    if (abs(dx) > max(abs(dy),abs(dz))) branchBlock = sideBranchBlockX;
                    else if (abs(dz) > max(abs(dy),abs(dx))) branchBlock = sideBranchBlockZ;
                    var branchLength = sideBranchLengthMin + randomInt(randPrecision)/randPrecision*sideBranchLengthVariation;
                    // Generate branch
                    for (var l = 0; l <= branchLength; l = l + 1) {
                        block(l*dx+ox, l*dy+oy, l*dz+oz, branchBlock);
                    }
                    // Rotate angle of next branch
                    branchAngle = branchAngle + goldenAngle;
                    // Generate leaf cluster at the end of the branch
                    double radius = sideBranchLeafRadiusMin;
                    double radiusSquared = pow(radius, 2);
                    // Leaf cluster origin (end of branch)
                    var lox = ox+branchLength*dx;
                    var loy = oy+branchLength*dy + sideBranchLeafTranslate;
                    var loz = oz+branchLength*dz;
                    for (var cx = ceil(-radius); cx <= ceil(radius); cx = cx + 1) {
                        for (var cy = ceil(-radius/sideBranchLeafBottomFlatness); cy <= ceil(radius/sideBranchLeafTopFlatness); cy = cy + 1) {
                            for (var cz = ceil(-radius); cz <= ceil(radius); cz = cz + 1) {
                                double squashFactor = sideBranchLeafBottomFlatness;
                                if (cy > 0) squashFactor = sideBranchLeafTopFlatness;
                                var distanceSquared = pow(cx,2)+pow(cy*squashFactor,2)+pow(cz,2);
                                if (distanceSquared < radiusSquared) {
                                    furniture(lox+cx, loy+cy, loz+cz, "eucalyptus_leaves");
                                }
                            }
                        }
                    }
                }
            }
            furniture(0, i, 0, "eucalyptus_log");
        }

        for (var b = 1; b <= topBranches; b = b + 1) {
            var t = b/topBranches;
            var ti = 1-t;
            // Branch direction vector
            double dx = sin(branchAngle) * t;
            double dy = topBranchVerticalDirection;
            double dz = cos(branchAngle) * t;
            // Normalize vector
            var mag = sqrt(pow(dx,2)+pow(dy,2)+pow(dz,2));
            dx = dx/mag;
            dy = dy/mag;
            dz = dz/mag;
            // Branch origin
            var ox = 0;
            var oy = trunkHeight;
            var oz = 0;
            // Set branch block based on largest unsigned vector component
            var branchBlock = topBranchBlockY;
            if (abs(dx) > max(abs(dy),abs(dz))) branchBlock = topBranchBlockX;
            else if (abs(dz) > max(abs(dy),abs(dx))) branchBlock = topBranchBlockZ;
            double branchLength = topBranchLengthUpwardsMin*ti + topBranchLengthOutwardsMin*t;
            // Generate branch
            for (var l = 0; l <= branchLength; l = l + 1) {
                block(l*dx+ox, l*dy+oy, l*dz+oz, branchBlock);
            }
            // Rotate angle of next branch
            branchAngle = branchAngle + goldenAngle;
            // Generate leaf cluster at the end of the branch
            double radius = topBranchLeafRadiusMin;
            double radiusSquared = pow(radius, 2);
            // Leaf cluster origin (end of branch)
            var lox = ox+branchLength*dx;
            var loy = oy+branchLength*dy + topBranchLeafTranslate;
            var loz = oz+branchLength*dz;
            for (var cx = ceil(-radius); cx <= ceil(radius); cx = cx + 1) {
                for (var cy = ceil(-radius/topBranchLeafBottomFlatness); cy <= ceil(radius/topBranchLeafTopFlatness); cy = cy + 1) {
                    for (var cz = ceil(-radius); cz <= ceil(radius); cz = cz + 1) {
                        var squashFactor = topBranchLeafBottomFlatness;
                        if (cy > 0) squashFactor = topBranchLeafTopFlatness;
                        var distanceSquared = pow(cx,2)+pow(cy*squashFactor,2)+pow(cz,2);
                        if (distanceSquared < radiusSquared && randomInt(25) != 0) {
                            furniture(lox+cx, loy+cy, loz+cz, "eucalyptus_leaves");
                        }
                    }
                }
            }
        }

        block(0, -1, 0, "minecraft:dirt");
    }
}
