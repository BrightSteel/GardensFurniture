package com.gardensmc.gardensfurniture.custom;

import com.gardensmc.gardensfurniture.events.FurnitureBreakEvent;
import com.gardensmc.gardensfurniture.register.CustomRegister;
import com.gardensmc.gardensfurniture.register.ItemRegistry;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomBlockManager {

    public static boolean attemptPlacement(CustomItem customItem, Location location, BlockFace blockFace) {
        CustomBlock customBlock = customItem.getCustomBlock();
        if (customBlock instanceof CustomNoteBlock customNoteBlock) {
            return placeNoteBlock(customNoteBlock, location);
        }
        else {
            return placeItemFrame(customItem, location, blockFace);
        }
    }

    private static boolean placeNoteBlock(CustomNoteBlock customNoteBlock, Location location) {
        Instrument instrument;
        try {
            instrument = Instrument.valueOf(customNoteBlock.getInstrument().toUpperCase());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        Block block = location.getBlock();
        block.setType(Material.NOTE_BLOCK);
        if (block.getBlockData() instanceof NoteBlock noteBlock) {
            noteBlock.setNote(new Note(customNoteBlock.getNote()));
            noteBlock.setInstrument(instrument);
            noteBlock.setPowered(customNoteBlock.isPowered());
            block.setBlockData(noteBlock);
        }
        return true;
    }

    private static boolean placeItemFrame(CustomItem customItem, Location location, BlockFace blockFace) {
        World world = location.getWorld();
        if (world != null) {

            ItemFrame itemFrame = (ItemFrame) location.getWorld().spawnEntity(location, EntityType.ITEM_FRAME);
            itemFrame.setFacingDirection(blockFace);
//            itemFrame.setFacingDirection();
            itemFrame.setVisible(false);
            ItemStack display = CustomRegister.INSTANCE.itemRegistry.buildItem(customItem);
            ItemMeta itemMeta = display.getItemMeta();
            if (itemMeta != null) {
                itemMeta.setDisplayName(null);
                display.setItemMeta(itemMeta);
            }
            itemFrame.setItem(display);

            if (customItem.getCustomBlock().hasBarrier()) {
                location.getBlock().setType(Material.BARRIER);
            }

            return true;
        }
        return false;
    }

    @Nullable
    public static CustomItem getItemFromNoteBlock(NoteBlock noteBlock) {
        int note = noteBlock.getNote().getId();
        String instrument = noteBlock.getInstrument().toString().toLowerCase();
        boolean powered = noteBlock.isPowered();
        String key = note + "," + instrument + "," + powered;
//        System.out.println("looking up " + note);

        return CustomRegister.INSTANCE.itemRegistry.getCustomItem(key);
    }

    @Nullable
    public static ItemFrame getBlock(Location location) {
        Collection<Entity> entities = location.getWorld().getNearbyEntities(location, 1, 1, 1);
        Block center = location.getBlock();

        for (Entity entity : entities) {
            if (entity instanceof ItemFrame itemFrame) {
                if (entity.getLocation().getBlock().getLocation().equals(center.getLocation()))
                    return itemFrame;
            }
        }

        return null;
    }

    public static void breakBlock(Player player, Location location) {
        ItemFrame itemFrame = getBlock(location);
        if (itemFrame == null) return;

        CustomItem customItem = CustomRegister.INSTANCE.itemRegistry.getCustomItem(itemFrame.getItem());
        if (customItem == null) return;

        itemFrame.remove();
        Block block = location.getBlock();
        if (block.getType() == Material.BARRIER) {
            block.setType(Material.AIR);
        }

        ItemStack itemStack = CustomRegister.INSTANCE.itemRegistry.buildItem(customItem);
        if (itemStack != null) {
            location.getWorld().dropItemNaturally(location, itemStack);
        }

        Bukkit.getPluginManager().callEvent(new FurnitureBreakEvent(player, customItem, itemFrame));

    }

    @Nullable
    public static CustomItem getFurniture(Entity entity) {
        if (entity instanceof ItemFrame itemFrame) {
            ItemStack itemStack = itemFrame.getItem();
            CustomItem customItem = CustomRegister.INSTANCE.itemRegistry.getCustomItem(itemStack);
            return customItem;
        }
        return null;
    }

}
