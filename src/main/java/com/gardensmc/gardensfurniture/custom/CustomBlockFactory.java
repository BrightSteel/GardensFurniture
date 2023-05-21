package com.gardensmc.gardensfurniture.custom;

import com.gardensmc.gardensfurniture.register.CustomRegister;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nullable;
import java.util.HashMap;

public class CustomBlockFactory {

    private final HashMap<String, String> customNoteBlocks;

    public CustomBlockFactory(HashMap<String, String> customNoteBlocks) {
        this.customNoteBlocks = customNoteBlocks;
    }

    // todo split each type into their own methods because this is super messy

    public CustomBlock createCustomBlock(String item_identifier, ConfigurationSection blockSection) {
        CustomBlock customBlock;
        String type = blockSection.getString("type");
        boolean seated = blockSection.getBoolean("seated");
        boolean cancel_drop = blockSection.getBoolean("cancel_drop");

        ConfigurationSection noteSection = blockSection.getConfigurationSection("note_data");
        ConfigurationSection leavesSection = blockSection.getConfigurationSection("leaves_data");
        if (type != null && type.equals("NOTE") && noteSection != null) {
            int note = noteSection.getInt("note");
            String instrument = noteSection.getString("instrument");
            boolean powered = noteSection.getBoolean("powered");
            customBlock = new CustomNoteBlock(note, instrument, powered, seated, cancel_drop);
            customNoteBlocks.put(note + "," + instrument + "," + powered, item_identifier);
        } else if (type != null && type.equals("LEAVES") && leavesSection != null) {
            int distance = leavesSection.getInt("distance");
            boolean persistent = leavesSection.getBoolean("persistent");
            customBlock = new CustomLeavesBlock(distance, persistent, seated, cancel_drop);
            customNoteBlocks.put(distance + "," + persistent, item_identifier);
        } else {
            boolean barrier = blockSection.getBoolean("barrier");
            customBlock = new CustomBlock(barrier, seated, cancel_drop);
        }

        if (blockSection.contains("seat_offset")) {
            customBlock.setSeatOffset(blockSection.getDouble("seat_offset"));
        }

        return customBlock;
    }
}
