package com.gardensmc.gardensfurniture.custom.block;

import org.bukkit.configuration.ConfigurationSection;

public class CustomBuddingBlock extends CustomNoteBlock {

    private final String crystalId;

    public CustomBuddingBlock(ConfigurationSection blockSection, String blockName) {
        super(blockSection, blockName);
        this.crystalId = blockSection.getString("crystal_id");
    }

    public String getCrystalId() {
        return crystalId;
    }
}
