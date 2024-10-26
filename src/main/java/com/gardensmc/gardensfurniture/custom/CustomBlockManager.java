package com.gardensmc.gardensfurniture.custom;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.CustomCrystalBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomTripwireBlock;
import com.gardensmc.gardensfurniture.custom.item.*;
import com.gardensmc.gardensfurniture.events.FurnitureBreakEvent;
import com.gardensmc.gardensfurniture.itementity.DisplayItemEntity;
import com.gardensmc.gardensfurniture.itementity.FrameItemEntity;
import com.gardensmc.gardensfurniture.itementity.ItemEntity;
import com.gardensmc.gardensfurniture.listener.BlockListener;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import com.gardensmc.gardensfurniture.util.LightUtil;
import com.gardensmc.gardensfurniture.util.ProtectionUtil;
import de.tr7zw.nbtapi.NBTEntity;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;

import static com.gardensmc.gardensfurniture.util.CustomDropUtil.dropCustomItem;

public class CustomBlockManager {

    public static boolean attemptPlacement(ItemStack heldItem, Player player, CustomPlaceableItem placeableItem, Location location, BlockFace blockFace, boolean persistent) {
        if (player != null) {
            if (!ProtectionUtil.canBuild(location, player)) {
                return false;
            }
        }

        if (entityBlocking(location)) {
            return false;
        }

        var material = location.getBlock().getType();
        if (!isPlaceableMaterial(material) && !BlockListener.isAmethystBud(material)) return false;
        // for terra gen, and also make sure we allow amethyst buds to place under the server-side bud

        return placeableItem.placeBlock(heldItem, player, location, blockFace, persistent);
    }

    public static boolean isPlaceableMaterial(Material material) {
        return material == Material.AIR
                || material == Material.LIGHT
                || material == Material.WATER
                || material == Material.SHORT_GRASS
                || material == Material.TALL_GRASS;
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
        String instrument = noteBlock.getInstrument().toString().toUpperCase();
        boolean powered = noteBlock.isPowered();
        String key = note + "," + instrument + "," + powered;
//        System.out.println("looking up " + note);

        return GardensFurniture.itemRegistry.getCustomItem(key);
    }

    @Nullable
    private static ItemEntity getItemEntity(Location location) {
        var displayEntity = getDisplayEntity(location);
        if (displayEntity != null) {
            return new DisplayItemEntity(displayEntity);
        }
        var itemFrame = getBlock(location);
        if (itemFrame != null) {
            return new FrameItemEntity(itemFrame);
        }
        return null;
    }

    @Nullable
    private static ItemFrame getBlock(Location location) {
        return (ItemFrame) getEntityByClass(location, ItemFrame.class);
    }

    @Nullable
    public static ItemDisplay getDisplayEntity(Location location) {
        return (ItemDisplay) getEntityByClass(location, ItemDisplay.class);
    }

    @Nullable
    public static Entity getEntityByClass(Location location, Class<? extends Entity> entityClass) {
        Collection<Entity> entities = location.getWorld().getNearbyEntities(location, 1, 1, 1);
        Block center = location.getBlock();

        for (Entity entity : entities) {
            if (entityClass.isInstance(entity)) {
                if (entity.getLocation().getBlock().getLocation().equals(center.getLocation()))
                    return entity;
            }
        }

        return null;
    }

    @Nullable
    public static CustomCrystalBlock getCustomCrystalFromPos(World world, int x, int y, int z) {
        Location location = new Location(world, x, y, z);
        var itemFrame = CustomBlockManager.getBlock(location);
        if (itemFrame != null) {
            var customItem = GardensFurniture.itemRegistry.getCustomItem(itemFrame.getItem());
            if (customItem instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomCrystalBlock crystalBlock) {
                return crystalBlock;
            }
        }
        return null;
    }

    public static void breakBlock(Player player, Location location) {
       if (!ProtectionUtil.canBuild(location, player)) {
           return;
       }

        ItemEntity itemEntity = getItemEntity(location);
        if (itemEntity == null) return;

        CustomItem customItem = GardensFurniture.itemRegistry.getCustomItem(itemEntity.getItem());
        if (customItem == null) return;

        FurnitureBreakEvent e = new FurnitureBreakEvent(player, (CustomPlaceableItem) customItem, itemEntity);
        Bukkit.getPluginManager().callEvent(e);
        if (e.isCancelled()) {
            return;
        }
        NBTEntity nbtEntity = new NBTEntity(itemEntity.getEntity());
        String display = null;
        if (nbtEntity.getPersistentDataContainer().hasKey("custom_name")) {
            display = nbtEntity.getPersistentDataContainer().getString("custom_name");
        }
        itemEntity.getEntity().remove();
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
//        LightUtil.addLightBack(block); - calls block break event already ?
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
        var furnitureStore = FurnitureStoreHandler.INSTANCE.getFurnitureStore(block);
        if (furnitureStore == null) {
            return null;
        }
        return GardensFurniture.itemRegistry.getItem(furnitureStore.getItemIdentifier());
    }

    public static Material[] packetTypes = new Material[] {
            Material.SPRUCE_LEAVES,
            Material.CHERRY_LEAVES,
            Material.CAVE_VINES,
            Material.CAVE_VINES_PLANT
    };

    @Nullable
    public static CustomItem getCustomItemFromBlock(Block block) {
        if (block.getType() == Material.NOTE_BLOCK && block.getBlockData() instanceof NoteBlock noteBlock) {
            return CustomBlockManager.getItemFromNoteBlock(noteBlock);
        }
        else if (block.getType() == Material.TRIPWIRE) {
            return CustomBlockManager.getItemFromLeaves(block);
        }
        else if (Arrays.asList(packetTypes).contains(block.getType())) {
            return CustomBlockManager.getItemFromLeaves(block);
        }

        ItemEntity itemEntity = getItemEntity(block.getLocation());
        if (itemEntity != null) {
            return GardensFurniture.itemRegistry.getCustomItem(itemEntity.getItem());
        }
        return null;
    }


}
