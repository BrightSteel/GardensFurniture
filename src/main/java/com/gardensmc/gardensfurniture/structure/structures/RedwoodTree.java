package com.gardensmc.gardensfurniture.structure.structures;

import com.gardensmc.gardensfurniture.structure.TStructure;

import static java.lang.Math.*;

public class RedwoodTree extends TStructure {

    @Override
    public void generate() {
        var randPrecision = 100;
        var goldenAngle = 2.39996;
        var pi = 3.14159;

        var trunkHeight = 30+randomInt(10);
        double trunkStartRadius = 2.5;
        double trunkMidRadius = 1.5;
        double trunkMidPoint = 0.1+ (double) randomInt(randPrecision) /randPrecision*0.2;
        double trunkEndRadius = 1.1;
        var trunkBlock = "minecraft:note_block[instrument=basedrum,note=16]";

        var sideBranchInterval = 1+randomInt(2);
        var sideBranchMinHeight = trunkHeight*(0.3+randomInt(randPrecision)/randPrecision/3);
        var sideBranchLength = (trunkHeight - sideBranchMinHeight)/(4+randomInt(2));
        var sideBranchVerticalVariation = 0;
        var sideBranchVerticalDirection = randomInt(randPrecision)/randPrecision-0.5;
        var sideBranchLeafRadiusMin = 3;
        var sideBranchLeafTranslate = 0;
        var sideBranchLeafTopFlatness = 2;
        var sideBranchLeafBottomFlatness = 2;
        var sideBranchLeafBlock = "redwood_leaves";
        var sideBranchBlockX = "minecraft:note_block[instrument=basedrum,note=17]";
        var sideBranchBlockY = "minecraft:note_block[instrument=basedrum,note=16]";
        var sideBranchBlockZ = "minecraft:note_block[instrument=basedrum,note=18]";

        var t = 0;
        var currentRadius = trunkStartRadius;
        var currentRadiusSquared = pow2(currentRadius);
        double distanceSquared = 0;
        var branchAngle = randomInt(randPrecision)/randPrecision*2*pi;
        for (var i = -2; i < trunkHeight; i = i + 1) {
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
                    var branchLength = -sideBranchLength*(i-trunkHeight)/(trunkHeight-sideBranchMinHeight);
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
                                var squashFactor = sideBranchLeafBottomFlatness;
                                if (cy > 0) squashFactor = sideBranchLeafTopFlatness;
                                distanceSquared = pow(cx,2)+pow(cy*squashFactor,2)+pow(cz,2);
                                if (distanceSquared/radiusSquared < (double) randomInt(randPrecision) /randPrecision) {
                                    furniture(lox+cx, loy+cy, loz+cz, sideBranchLeafBlock);
                                }
                            }
                        }
                    }
                }
            }

            t = i/trunkHeight;
            if (t < trunkMidPoint) currentRadius = t*(-trunkStartRadius+trunkMidRadius)/trunkMidPoint+trunkStartRadius;
            else currentRadius = (t-trunkMidPoint)*(trunkEndRadius-trunkMidRadius)/(1-trunkMidPoint)+trunkMidRadius;
            currentRadiusSquared = pow2(currentRadius);
            for (var x = round(-currentRadius); x < round(currentRadius); x = x + 1) {
                for (var z = round(-currentRadius); z < round(currentRadius); z = z + 1) {
                    distanceSquared = pow2(x)+pow2(z);
                    if (distanceSquared < currentRadiusSquared) {
                        block(x, i, z, trunkBlock);
                    }
                }
            }
        }

        block(0, trunkHeight, 0, trunkBlock);
        furniture(0, trunkHeight+1, 0, sideBranchLeafBlock);
        furniture(1, trunkHeight+1, 0, sideBranchLeafBlock);
        furniture(-1, trunkHeight+1, 0, sideBranchLeafBlock);

        furniture(0, trunkHeight+1, 1, sideBranchLeafBlock);

        furniture(0, trunkHeight+1, -1, sideBranchLeafBlock);

        furniture(0, trunkHeight+2, 0, sideBranchLeafBlock);

        furniture(0, trunkHeight+3, 0, sideBranchLeafBlock);

    }
}
