package com.gardensmc.gardensfurniture.custom;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.*;
import com.gardensmc.gardensfurniture.custom.item.*;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.events.CustomBlockPlaceEvent;
import com.gardensmc.gardensfurniture.events.FurnitureBreakEvent;
import com.gardensmc.gardensfurniture.events.FurniturePlaceEvent;
import com.gardensmc.gardensfurniture.util.DirectionUtil;
import com.gardensmc.gardensfurniture.util.ItemFrameUtil;
import com.gardensmc.gardensfurniture.util.ProtectionUtil;
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

    public static boolean attemptPlacement(Player player, CustomPlaceableItem placeableItem, Location location, BlockFace blockFace, boolean persistent) {
        if (player != null) {
            if (!ProtectionUtil.canBuild(location, player)) {
                return false;
            }
        }

        if (entityBlocking(location)) {
            return false;
        }

        if (location.getBlock().getType() != Material.AIR) return false; // for terra gen

        return placeableItem.placeBlock(player, location, blockFace, persistent);
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

    @Nullable
    public static CustomItem getItemFromNoteBlock(NoteBlock noteBlock) {
        int note = noteBlock.getNote().getId();
        String instrument = noteBlock.getInstrument().toString().toLowerCase();
        boolean powered = noteBlock.isPowered();
        String key = note + "," + instrument + "," + powered;
//        System.out.println("looking up " + note);

        return GardensFurniture.itemRegistry.getCustomItem(key);
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
       if (!ProtectionUtil.canBuild(location, player)) {
           return;
       }

        ItemFrame itemFrame = getBlock(location);
        if (itemFrame == null) return;

        CustomItem customItem = GardensFurniture.itemRegistry.getCustomItem(itemFrame.getItem());
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

        ItemStack itemStack = GardensFurniture.itemRegistry.buildItem(customItem);
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
            CustomItem customItem = GardensFurniture.itemRegistry.getCustomItem(itemStack);
            return customItem;
        }
        return null;
    }

    @Nullable
    public static CustomItem getItemFromLeaves(Block block) {
        String identifier = block.getChunk().getPersistentDataContainer().get(new NamespacedKey("gardens", block.getX() + "." + block.getY() + "." + block.getZ()), PersistentDataType.STRING);
        if (identifier != null) {
            String[] parts = identifier.split("\\.");
            return GardensFurniture.itemRegistry.getItem(parts[0]);
        }

        return null;
    }

}
