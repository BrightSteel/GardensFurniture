package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.events.CustomBlockPlaceEvent;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.util.LightUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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


    @Override
    public boolean isSolid() {
        return true;
    }

    @Override
    public boolean placeBlock(ItemStack heldItem, CustomPlaceableItem customItem, Player player, Location location, BlockFace blockFace, boolean persistent, FurnitureStore furnitureStore) {
        Block block = location.getBlock();
        CustomBlockPlaceEvent event = new CustomBlockPlaceEvent(player, customItem, block);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        Instrument instrument;
        try {
            instrument = Instrument.valueOf(getInstrument().toUpperCase());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        block.setType(Material.NOTE_BLOCK);
        if (block.getBlockData() instanceof NoteBlock noteBlock) {
            noteBlock.setNote(new Note(getNote()));
            noteBlock.setInstrument(instrument);
            noteBlock.setPowered(isPowered());
            block.setBlockData(noteBlock);
        }

        if (getLight() > 0) {
            LightUtil.placeLight(location, getLight(), true);
        }
        return true;
    }
}
