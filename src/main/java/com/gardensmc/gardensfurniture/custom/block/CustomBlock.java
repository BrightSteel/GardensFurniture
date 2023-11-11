package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public abstract class CustomBlock {

    private final boolean barrier;
    private final boolean seated;
    private int[] seats;
    private final boolean cancel_drop;

    private double seat_offset;

    public CustomBlock(ConfigurationSection blockSection) {
        this.barrier = blockSection.getBoolean("barrier");
        this.seated = blockSection.getBoolean("seated");
        this.cancel_drop = blockSection.getBoolean("cancel_drop");
        if (blockSection.contains("seat_offset")) {
            this.seat_offset = blockSection.getDouble("seat_offset");
        }
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

    protected ConfigurationSection getConfigSectionOrThrow(ConfigurationSection config, String sectionName, String customBlockName) {
        if (!config.isConfigurationSection(sectionName)) {
            throw new RuntimeException(String.format("No %s section exists for custom block %s", sectionName, customBlockName));
        }
        return config.getConfigurationSection(sectionName);
    }

    public abstract boolean placeBlock(CustomPlaceableItem placeableItem, Player player, Location location, BlockFace blockFace, boolean persistent);
}
