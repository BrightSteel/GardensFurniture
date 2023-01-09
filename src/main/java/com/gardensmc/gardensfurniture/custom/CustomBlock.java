package com.gardensmc.gardensfurniture.custom;

import org.bukkit.Location;

public class CustomBlock {
    public static CustomBlock EMPTY = null;

    private boolean barrier, seated;
    private int[] seats;
    private boolean cancel_drop;

    private double seat_offset;

    public CustomBlock(boolean seated, boolean cancel_drop) {
        this.seated = seated;
        this.cancel_drop = cancel_drop;
    }

    public CustomBlock(boolean barrier, boolean seated, boolean cancel_drop) {
        this.barrier = barrier;
        this.seated = seated;
        this.cancel_drop = cancel_drop;
    }

    public boolean hasBarrier() {
        return barrier;
    }

    public boolean cancelDrop() {
        return cancel_drop;
    }

    public boolean isSeated() {
        return seated;
    }

    public void setSeatOffset(double offset) {
        this.seat_offset = offset;
    }

    public double getSeatOffset() {
        return seat_offset;
    }
}
