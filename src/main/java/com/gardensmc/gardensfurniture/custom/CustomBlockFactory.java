package com.gardensmc.gardensfurniture.custom;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.*;
import com.gardensmc.gardensfurniture.custom.type.CustomBlockType;
import org.bukkit.configuration.ConfigurationSection;

public class CustomBlockFactory {

    public CustomBlock createCustomBlock(String item_identifier, ConfigurationSection blockSection) {
        String type = blockSection.getString("type");
        if (type == null) {
            throw new RuntimeException("No type specified for customBlock: " + item_identifier);
        }

        CustomBlock customBlock;
        switch(CustomBlockType.valueOf(type.toUpperCase())) {
            case CAVE_VINE -> customBlock = new CustomCaveVineBlock(blockSection, item_identifier);
            case TRIPWIRE -> customBlock = new CustomTripwireBlock(blockSection, item_identifier);
            case NOTE -> {
                CustomNoteBlock customNoteBlock = new CustomNoteBlock(blockSection, item_identifier);
                GardensFurniture.itemRegistry.customNoteBlocks.put(customNoteBlock.getNote() + "," + customNoteBlock.getInstrument() + "," + customNoteBlock.isPowered(), item_identifier);
                customBlock = customNoteBlock;
            }
            case LEAVES -> {
                CustomLeavesBlock customLeavesBlock = new CustomLeavesBlock(blockSection, item_identifier);
                GardensFurniture.itemRegistry.customNoteBlocks.put(customLeavesBlock.getDistance() + "," + customLeavesBlock.isPersistent(), item_identifier);
                customBlock = customLeavesBlock;
            }
            case ITEM_FRAME -> customBlock = new CustomItemFrameBlock(blockSection);
            default -> throw new RuntimeException("Unknown customBlockType: " + type);
        }
        return customBlock;
    }
}
