package com.gardensmc.gardensfurniture.custom.block;

import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nullable;

public class CustomLeavesBlock extends CustomBlock {

    private final int distance;
    private final boolean persistent;

    private final String sapling;

    public CustomLeavesBlock(ConfigurationSection blockSection, String customBlockName) {
        super(blockSection);
        ConfigurationSection leavesSection = getConfigSectionOrThrow(blockSection, "leaves_data", customBlockName);
        this.distance = leavesSection.getInt("distance");
        this.persistent = leavesSection.getBoolean("persistent");
        this.sapling = blockSection.getString("sapling");
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
