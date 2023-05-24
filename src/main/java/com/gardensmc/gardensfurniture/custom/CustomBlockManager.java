package com.gardensmc.gardensfurniture.custom;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.database.LeavesData;
import com.gardensmc.gardensfurniture.events.CustomBlockPlaceEvent;
import com.gardensmc.gardensfurniture.events.FurnitureBreakEvent;
import com.gardensmc.gardensfurniture.events.FurniturePlaceEvent;
import com.gardensmc.gardensfurniture.listener.S2CListener;
import com.gardensmc.gardensfurniture.register.CustomRegister;
import com.gardensmc.gardensfurniture.util.ItemFrameUtil;
import de.tr7zw.nbtapi.NBTEntity;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.util.Collection;

public class CustomBlockManager {

    public static boolean attemptPlacement(Player player, CustomItem customItem, Location location, BlockFace blockFace) {
        CustomBlock customBlock = customItem.getCustomBlock();
        if (customItem instanceof CustomDirectionalItem customDirectionalItem) {
            switch (blockFace) {
                case UP, DOWN -> customBlock = customDirectionalItem.getY();
                case EAST, WEST -> customBlock = customDirectionalItem.getX();
                case NORTH, SOUTH -> customBlock = customDirectionalItem.getZ();
            }
        }

        return attemptPlacement(player, customItem, location, blockFace, customBlock);
    }

    private static boolean attemptPlacement(Player player, CustomItem customItem, Location location, BlockFace blockFace, CustomBlock customBlock) {
        if (entityBlocking(location)) {
            return false;
        }

        if (customBlock instanceof CustomNoteBlock customNoteBlock) {
            return placeNoteBlock(player, customItem, customNoteBlock, location);
        }
        else if (customBlock instanceof CustomLeavesBlock customLeavesBlock) {
            return placeLeavesBlock(player, customItem, customLeavesBlock, location);
        }
        else {
            return placeItemFrame(player, customItem, location, blockFace);
        }
    }

    private static boolean entityBlocking(Location loc) {
        if (loc.getWorld() == null) {
            for (Entity entity : loc.getWorld().getNearbyEntities(loc, 1, 1, 1)) {
                if (entity.getLocation().getBlock().equals(loc.getBlock()) && entity instanceof LivingEntity && !(entity instanceof ItemFrame)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean placeLeavesBlock(Player player, CustomItem customItem, CustomLeavesBlock customLeavesBlock, Location location) {
        Block block = location.getBlock();
        block.setType(Material.SPRUCE_LEAVES);
        try {
            LeavesData.INSTANCE.addLeave(block, customItem.getIdentifier());
//            GardensFurniture.leavesMap.get(block.getWorld().getName()).refreshChunk(block.getChunk());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (block.getBlockData() instanceof Leaves leaves) {
            leaves.setPersistent(true);
            block.setBlockData(leaves);
        }

        Bukkit.getPluginManager().callEvent(new CustomBlockPlaceEvent(player, customItem, block));

        return true;
    }


    private static boolean placeNoteBlock(Player player, CustomItem customItem, CustomNoteBlock customNoteBlock, Location location) {

        Block block = location.getBlock();
        CustomBlockPlaceEvent event = new CustomBlockPlaceEvent(player, customItem, block);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        Instrument instrument;
        try {
            instrument = Instrument.valueOf(customNoteBlock.getInstrument().toUpperCase());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        block.setType(Material.NOTE_BLOCK);
        if (block.getBlockData() instanceof NoteBlock noteBlock) {
            noteBlock.setNote(new Note(customNoteBlock.getNote()));
            noteBlock.setInstrument(instrument);
            noteBlock.setPowered(customNoteBlock.isPowered());
            block.setBlockData(noteBlock);
        }

        return true;
    }

    private static boolean placeItemFrame(Player player, CustomItem customItem, Location location, BlockFace blockFace) {
        World world = location.getWorld();
        if (world != null) {

            ItemFrame itemFrame = (ItemFrame) location.getWorld().spawnEntity(location, EntityType.ITEM_FRAME);
            itemFrame.setVisible(false);
            itemFrame.setFacingDirection(blockFace, true);
            ItemStack display = CustomRegister.INSTANCE.itemRegistry.buildItem(customItem);
            String custom_name = null;
            ItemMeta itemMeta = display.getItemMeta();
            if (itemMeta != null) {
                custom_name = itemMeta.getDisplayName();
                itemMeta.setDisplayName(null);
                display.setItemMeta(itemMeta);
            }
            itemFrame.setItem(display);
            itemFrame.setRotation(ItemFrameUtil.getRotationFromFacing(player));
            if (custom_name != null) {
                NBTEntity nbtEntity = new NBTEntity(itemFrame);
                nbtEntity.getPersistentDataContainer().setString("custom_name", custom_name);
            }

            if (customItem.getCustomBlock().hasBarrier()) {
                location.getBlock().setType(Material.BARRIER);
            }

            Bukkit.getPluginManager().callEvent(new FurniturePlaceEvent(null, customItem, itemFrame));

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

        FurnitureBreakEvent e = new FurnitureBreakEvent(player, customItem, itemFrame);
        Bukkit.getPluginManager().callEvent(e);
        if (e.isCancelled()) {
            return;
        }
        NBTEntity nbtEntity = new NBTEntity(itemFrame);
        String display = null;
        if (nbtEntity.getPersistentDataContainer().hasKey("custom_name")) {
            display = nbtEntity.getPersistentDataContainer().getString("custom_name");
        }
        itemFrame.remove();
        Block block = location.getBlock();
        if (block.getType() == Material.BARRIER) {
            block.setType(Material.AIR);
        }

        ItemStack itemStack = CustomRegister.INSTANCE.itemRegistry.buildItem(customItem);
        if (itemStack != null) {
            if (display != null && itemStack.getItemMeta() != null) {
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.setDisplayName(display);
                itemStack.setItemMeta(itemMeta);
            }
            location.getWorld().dropItemNaturally(location, itemStack);
        }



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

    @Nullable
    public static CustomItem getItemFromLeaves(Block block) {
        String identifier = block.getChunk().getPersistentDataContainer().get(new NamespacedKey("gardens", block.getX() + "." + block.getY() + "." + block.getZ()), PersistentDataType.STRING);
        if (identifier != null) {
            return CustomRegister.INSTANCE.itemRegistry.getItem(identifier);
        }

        return null;
    }

}
