package com.gardensmc.gardensfurniture.custom;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class CustomTripwireBlock extends CustomBlock {

    private boolean powered;
    private boolean attached;
    private boolean north, south, east, west, disarmed;
    private boolean large;
    private List<Material> placeable = new ArrayList<>();

    public CustomTripwireBlock(boolean north, boolean south, boolean east, boolean west, boolean disarmed, boolean powered, boolean attached, boolean large, List<String> placeable, boolean seated, boolean cancel_drop) {
        super(seated, cancel_drop);
        this.north = north;
        this.south = south;
        this.east = east;
        this.west = west;
        this.disarmed = disarmed;
        this.powered = powered;
        this.attached = attached;
        this.large = large;
        setPlaceable(placeable);
    }

    /**
     * List of materials that this can be placed on
     * @return empty list if none
     */
    public List<Material> getPlaceable() {
        return placeable;
    }

    public void setPlaceable(List<String> placeable) {
        for (String str : placeable) {
            try {
                this.placeable.add(Material.valueOf(str.toUpperCase()));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public boolean isNorth() {
        return north;
    }

    public boolean isSouth() {
        return south;
    }

    public boolean isEast() {
        return east;
    }

    public boolean isWest() {
        return west;
    }

    public boolean isDisarmed() {
        return disarmed;
    }

    public boolean isPowered() {
        return powered;
    }

    public boolean isAttached() {
        return attached;
    }

    public boolean isLarge() {
        return large;
    }
}
