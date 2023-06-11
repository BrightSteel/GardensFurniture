package com.gardensmc.gardensfurniture.custom;

import javax.annotation.Nullable;

public class CustomLeavesBlock extends CustomBlock {

    private int distance;
    private boolean persistent;

    private String sapling;

    public CustomLeavesBlock(String sapling, int distance, boolean persistent, boolean seated, boolean cancel_drop) {
        super(seated, cancel_drop);
        this.distance = distance;
        this.persistent = persistent;
        this.sapling = sapling;
    }

    public int getDistance() {
        return distance;
    }

    public boolean isPersistent() {
        return persistent;
    }

    @Nullable
    public String getSapling() {
        return sapling;
    }
}
