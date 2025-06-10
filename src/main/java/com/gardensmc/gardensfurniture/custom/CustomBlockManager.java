package com.gardensmc.gardensfurniture.custom;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.*;
import com.gardensmc.gardensfurniture.events.FurnitureBreakEvent;
import com.gardensmc.gardensfurniture.itementity.DisplayItemEntity;
import com.gardensmc.gardensfurniture.itementity.FrameItemEntity;
import com.gardensmc.gardensfurniture.itementity.ItemEntity;
import com.gardensmc.gardensfurniture.mapletap.MapleTapHandler;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import com.gardensmc.gardensfurniture.util.ProtectionUtil;
import de.tr7zw.nbtapi.NBTEntity;
import dev.geco.gsit.api.GSitAPI;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;

public class CustomBlockManager {

    /**
     * Performs additional checks before placing block.
     * Checks:
     *  • Build perms
     *  • Location is not blocked by entities
     *  • Location is replaceable material (air, grass, etc)
     * @return placement success
     */
    public static boolean attemptPlacement(Player player, ItemStack heldItem, CustomPlaceableItem placeableItem, Location location, PlaceOptions placeOptions) {
        if (!ProtectionUtil.canBuild(location, player)) {
            return false;
        }
        if (entityBlocking(location)) {
            return false;
        }
        var material = location.getBlock().getType();
        if (!isPlaceableMaterial(material)) return false;
        return placeableItem.placeBlock(player, heldItem, location, placeOptions);
    }

    public static boolean isPlaceableMaterial(Material material) {
        return material == Material.AIR
                || material == Material.LIGHT
                || material == Material.WATER
                || material == Material.SHORT_GRASS
                || material == Material.TALL_GRASS;
    }

    private static boolean entityBlocking(Location loc) {
        if (loc.getWorld() != null) {
            return loc.getWorld().getNearbyEntities(loc, 1, 1, 1, entity -> entity instanceof LivingEntity || entity instanceof Display)
                    .stream()
                    // ignore display entity data holders
                    .filter(entity -> !(entity instanceof ItemDisplay itemDisplay && itemDisplay.getItemStack() == null))
                    .anyMatch(entity -> isValidEntity(entity, loc));
        }
        return false;
    }

    private static boolean isValidEntity(Entity entity, Location loc) {
        return entity.getLocation().getBlock().equals(loc.getBlock());
    }

    // todo implement for model engine display
    @Nullable
    public static CustomItem getItemFromDisplay(ItemDisplay itemDisplay) {
        return GardensFurniture.itemRegistry.getCustomItem(itemDisplay.getItemStack());
    }

    @Nullable
    public static ItemEntity getItemEntity(Location location) {
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
    public static Interaction getInteractionEntity(Location location) {
        return (Interaction) getEntityByClass(location, Interaction.class);
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

    public static void breakFurniture(Player player, Location location) {
       if (player != null && !ProtectionUtil.canBuild(location, player)) {
           return;
       }
        ItemEntity itemEntity = getItemEntity(location);
        if (itemEntity == null) return;

        CustomItem customItem = GardensFurniture.itemRegistry.getCustomItem(itemEntity.getItem());
        FurnitureBreakEvent e = new FurnitureBreakEvent(player, (CustomPlaceableItem) customItem, itemEntity);
        Bukkit.getPluginManager().callEvent(e);
        if (e.isCancelled()) {
            return;
        }

        var placeable = (CustomPlaceableItem) customItem;
        ItemStack itemStack = getDrop(itemEntity, placeable);
        if (player != null && player.getGameMode() != GameMode.CREATIVE && itemStack != null) {
            location.getWorld().dropItemNaturally(location, itemStack);
        }

        // remove furniture entities / hitbox
        itemEntity.getEntity().remove();
        var interactionEntity = getInteractionEntity(location);
        if (interactionEntity != null) {
            interactionEntity.remove();
        }
        Block block = location.getBlock();
        if (block.getType() == Material.BARRIER) {
            block.setType(Material.AIR);
        }
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
            Material.OAK_LEAVES,
            Material.SPRUCE_LEAVES,
            Material.CHERRY_LEAVES,
            Material.NOTE_BLOCK,
            Material.CAVE_VINES,
            Material.CAVE_VINES_PLANT,
            Material.TRIPWIRE
    };

    @Nullable
    public static CustomItem getCustomItemFromBlock(Block block) {
        if (Arrays.asList(packetTypes).contains(block.getType())) {
            return CustomBlockManager.getItemFromLeaves(block);
        }

        ItemEntity itemEntity = getItemEntity(block.getLocation());
        if (itemEntity != null) {
            return GardensFurniture.itemRegistry.getCustomItem(itemEntity.getItem());
        }
        return null;
    }

    public static ItemStack getDrop(ItemEntity itemEntity, CustomPlaceableItem customPlaceableItem) {
        var block = customPlaceableItem.getCustomBlock();
        if (block.cancelDrop()) {
            return null;
        }
        var override = block.getDropOverride();
        if (override != null) {
            return GardensFurniture.itemRegistry.buildItem(override);
        }
        return itemEntity.getItem();
    }

    public static void onInteractFurniture(Player player, EquipmentSlot hand, ItemEntity itemEntity) {
        ItemStack heldItem = player.getInventory().getItem(hand);
        CustomItem customItem = GardensFurniture.itemRegistry.getCustomItem(itemEntity.getItem());
        if (!(customItem instanceof CustomPlaceableItem customBlockItem)) {
            return;
        }
        if (player.isSneaking()) {
            itemEntity.rotate(45);
        } else if (heldItem.getType().toString().toUpperCase().endsWith("DYE") && customItem.isDyeable() && itemEntity.getItem().getItemMeta() instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(getColorFromDye(heldItem.getType()));
            var item = itemEntity.getItem();
            item.setItemMeta(leatherMeta);
            itemEntity.setItem(item);
            heldItem.setAmount(heldItem.getAmount() - 1);
        } else if (customBlockItem.getCustomBlock().isSeated()) {
            double offset = customBlockItem.getCustomBlock().getSeatOffset();
            GSitAPI.createSeat(itemEntity.getEntity().getLocation().getBlock(), player, true, 0.0, offset, 0.0, player.getLocation().getYaw(), true);
        } else if (customItem.getIdentifier().equals("maple_tap_5") && heldItem.getType() == Material.GLASS_BOTTLE) {
            itemEntity.setItem(GardensFurniture.itemRegistry.buildItem("maple_tap_1"));
            var nbt = new NBTEntity(itemEntity.getEntity()).getPersistentDataContainer();
            nbt.removeKey(MapleTapHandler.TAP_NBT);
            var mapleSyrup = GardensFurniture.itemRegistry.buildItem("maple_syrup");
            if (mapleSyrup == null) {
                Bukkit.getLogger().log(Level.SEVERE, "Failed to get maple_syrup item!");
                return;
            }
            if (heldItem.getAmount() > 1) {
                heldItem.setAmount(heldItem.getAmount() - 1);
                player.getInventory().addItem(mapleSyrup);
            } else {
                player.getInventory().setItem(hand, mapleSyrup);
            }
        } else {
            // todo update this to accept itemEntity
//                    Bukkit.getPluginManager().callEvent(new FurnitureInteractEvent(e.getPlayer(), customItem, itemEntity, e.getHand(), heldItem));
        }
    }

    private static Color getColorFromDye(Material material) {
        Color color;
        switch (material) {
            case BLACK_DYE -> color = Color.BLACK;
            case BLUE_DYE -> color = Color.BLUE;
            case BROWN_DYE -> color = Color.fromRGB(160,82,45);
            case CYAN_DYE -> color = Color.AQUA;
            case GRAY_DYE -> color = Color.GRAY;
            case GREEN_DYE -> color = Color.GREEN;
            case LIGHT_BLUE_DYE -> color = Color.fromRGB(137, 207, 240);
            case LIGHT_GRAY_DYE -> color = Color.SILVER;
            case LIME_DYE -> color = Color.LIME;
            case MAGENTA_DYE -> color = Color.FUCHSIA;
            case ORANGE_DYE -> color = Color.ORANGE;
            case PINK_DYE -> color = Color.fromRGB(255,192,203);
            case PURPLE_DYE -> color = Color.PURPLE;
            case RED_DYE -> color = Color.RED;
            case WHITE_DYE -> color = Color.WHITE;
            case YELLOW_DYE -> color = Color.YELLOW;
            default -> throw new RuntimeException("Unknown dye: " + material);
        }
        return color;
    }


}
