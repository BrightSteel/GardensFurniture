package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.item.PlaceOptions;
import com.gardensmc.gardensfurniture.events.CustomBlockPlaceEvent;
import com.gardensmc.gardensfurniture.util.LightUtil;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Getter
public class CustomNoteBlock extends CustomBlock {

    private final Note note;
    private final Instrument instrument;
    private final boolean powered;

    public CustomNoteBlock(ConfigurationSection blockSection, String blockName) {
        super(blockSection);
        ConfigurationSection noteSection = getConfigSectionOrThrow(blockSection, "note_data", blockName);
        this.note = new Note(noteSection.getInt("note"));
        this.instrument = Instrument.valueOf(noteSection.getString("instrument").toUpperCase());
        this.powered = noteSection.getBoolean("powered");
    }


    @Override
    public boolean isSolid() {
        return true;
    }

    @Override
    public boolean placeBlock(Player player, ItemStack heldItem, CustomPlaceableItem customItem, Location location, PlaceOptions placeOptions) {
        Block block = location.getBlock();
        CustomBlockPlaceEvent event = new CustomBlockPlaceEvent(player, customItem, block);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        block.setType(Material.NOTE_BLOCK);
        if (block.getBlockData() instanceof NoteBlock noteBlock) {
            noteBlock.setNote(note);
            noteBlock.setInstrument(instrument);
            noteBlock.setPowered(powered);
            block.setBlockData(noteBlock);
        }

        if (getLight() > 0) {
            LightUtil.placeLight(location, getLight(), true);
        }
        return true;
    }

    public boolean isNoteBlock(NoteBlock noteBlock) {
        return noteBlock.getInstrument() == instrument
                && noteBlock.getNote().equals(note)
                && noteBlock.isPowered() == isPowered();
    }
}
