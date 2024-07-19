package com.gardensmc.gardensfurniture.custom.block;

import org.bukkit.configuration.ConfigurationSection;

public class CustomCrystalBlock extends CustomItemFrameBlock {

    private final String crystalId;
    private final CrystalType type;

    public CustomCrystalBlock(ConfigurationSection blockSection, String customBlockName) {
        super(blockSection);
        var crystalSection = blockSection.getConfigurationSection("crystal_data");
        if (crystalSection == null) {
            throw new RuntimeException("Missing crystal section for block: " + customBlockName);
        }
        this.crystalId = crystalSection.getString("id");
        this.type = CrystalType.valueOf(crystalSection.getString("type").toUpperCase());
    }

    public CrystalType getType() {
        return type;
    }

    public String getCrystalId() {
        return crystalId;
    }

    public enum CrystalType {
        SMALL_BUD,
        MEDIUM_BUD,
        LARGE_BUD,
        CLUSTER
    }
}


