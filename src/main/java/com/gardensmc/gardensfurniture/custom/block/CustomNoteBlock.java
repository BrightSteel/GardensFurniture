package com.gardensmc.gardensfurniture.custom.block;

import org.bukkit.configuration.ConfigurationSection;

public class CustomNoteBlock extends CustomBlock {

    private final int note;
    private final String instrument;
    private final boolean powered;

    public CustomNoteBlock(ConfigurationSection blockSection, String blockName) {
        super(blockSection);
        ConfigurationSection noteSection = getConfigSectionOrThrow(blockSection, "note_data", blockName);
        this.note = noteSection.getInt("note");
        this.instrument = noteSection.getString("instrument");
        this.powered = noteSection.getBoolean("powered");
    }

    public int getNote() {
        return note;
    }

    public boolean isPowered() {
        return powered;
    }

    public String getInstrument() {
        return instrument;
    }


}
