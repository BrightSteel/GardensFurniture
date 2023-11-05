package com.gardensmc.gardensfurniture.custom.block;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class CustomTripwireBlock extends CustomBlock {

    private final boolean powered;
    private final boolean attached;
    private final boolean north;
    private final boolean south;
    private final boolean east;
    private final boolean west;
    private final boolean disarmed;
    private final boolean large;
    private final List<Material> placeable = new ArrayList<>();

    public CustomTripwireBlock(ConfigurationSection blockSection, String customBlockName) {
        super(blockSection);
        ConfigurationSection tripWireSection = getConfigSectionOrThrow(blockSection, "tripwire_data", customBlockName);
        this.north = tripWireSection.getBoolean("north");
        this.south = tripWireSection.getBoolean("south");
        this.east = tripWireSection.getBoolean("east");
        this.west = tripWireSection.getBoolean("west");
        this.disarmed = tripWireSection.getBoolean("disarmed");
        this.powered = tripWireSection.getBoolean("powered");
        this.attached = tripWireSection.getBoolean("attached");
        this.large = blockSection.getBoolean("large");
        setPlaceable(blockSection.getStringList("placeable"));
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
