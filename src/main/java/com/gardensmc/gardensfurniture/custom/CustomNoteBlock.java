package com.gardensmc.gardensfurniture.custom;

import org.bukkit.Instrument;
import org.bukkit.Note;

public class CustomNoteBlock extends CustomBlock {

    private int note;
    private String instrument;
    private boolean powered;

    public CustomNoteBlock(int note, String instrument, boolean powered, boolean seated, boolean cancel_drop) {
        super(seated, cancel_drop);
        this.note = note;
        this.instrument = instrument;
        this.powered = powered;
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
