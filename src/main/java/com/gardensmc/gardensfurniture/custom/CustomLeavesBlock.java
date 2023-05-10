package com.gardensmc.gardensfurniture.custom;

public class CustomLeavesBlock extends CustomBlock {

    private int distance;
    private boolean persistent;

    public CustomLeavesBlock(int distance, boolean persistent, boolean seated, boolean cancel_drop) {
        super(seated, cancel_drop);
        this.distance = distance;
        this.persistent = persistent;
    }

    public int getDistance() {
        return distance;
    }

    public boolean isPersistent() {
        return persistent;
    }
}
