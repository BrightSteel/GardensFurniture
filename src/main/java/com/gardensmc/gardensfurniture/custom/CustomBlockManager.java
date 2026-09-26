package com.gardensmc.gardensfurniture.custom;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.Barriers;
import com.gardensmc.gardensfurniture.custom.block.CustomDisplayBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomTripwireBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomWaxedBlock;
import com.gardensmc.gardensfurniture.custom.item.*;
import com.gardensmc.gardensfurniture.events.Furniture;
import com.gardensmc.gardensfurniture.events.FurnitureBreakEvent;
import com.gardensmc.gardensfurniture.events.FurnitureInteractEvent;
import com.gardensmc.gardensfurniture.itementity.DisplayItemEntity;
import com.gardensmc.gardensfurniture.itementity.ItemEntity;
import com.gardensmc.gardensfurniture.mapletap.MapleTapHandler;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import com.gardensmc.gardensfurniture.util.ProtectionUtil;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.NBTEntity;
import dev.geco.gsit.api.GSitAPI;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Stream;

public class CustomBlockManager {

    /**
     * Performs additional checks before placing block.
     * Checks:
     *  • Build perms
     *  • Location is not blocked by entities
     *  • Location is replaceable material (air, grass, etc)
     * @return placement success
     */
    public static boolean attemptPlacement(Player player, ItemStack heldItem, CustomPlaceableItem placeableItem, Location location, PlaceOptions placeOptions, Block clickedBlock) {
        if (!ProtectionUtil.canBuild(location, player)) {
            return false;
        }
        if (entityBlocking(location)) {
            return false;
        }
        var material = location.getBlock().getType();
        if (!isPlaceableMaterial(material)) return false;
        // Expose the placing player as the furniture lifecycle cause (for Prism attribution).
        FurnitureStoreHandler.currentActor.set(player);
        try {
            return placeableItem.placeBlock(player, heldItem, location, placeOptions, clickedBlock);
        } finally {
            FurnitureStoreHandler.currentActor.remove();
        }
    }

    public static boolean isPlaceableMaterial(Material material) {
        return material == Material.AIR
                || material == Material.LIGHT
                || material == Material.WATER
                || material == Material.SHORT_GRASS
                || material == Material.TALL_GRASS
                || material == Material.SNOW;
    }

    public static boolean entityBlocking(Location loc) {
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
        return getItemEntity(location, false);
    }

    @Nullable
    public static ItemEntity getItemEntity(Location location, boolean isCheckAdjacentEntities) {
        var displayEntity = getDisplayEntity(location, isCheckAdjacentEntities);
        if (displayEntity != null) {
            return new DisplayItemEntity(displayEntity);
        }
        return null;
    }

    @Nullable
    public static ItemDisplay getDisplayEntity(Location location) {
        return getDisplayEntity(location, false);
    }

    @Nullable
    public static ItemDisplay getDisplayEntity(Location location, boolean isCheckAdjacent) {
        return getDisplayEntity(location, isCheckAdjacent, display -> true);
    }

    /**
     * Like {@link #getItemEntity(Location, boolean)} but only matches placed furniture, never another
     * plugin's display (or our own sapling/pot model displays). Use for break and interact handling.
     */
    @Nullable
    public static ItemEntity getFurnitureEntity(Location location) {
        return getFurnitureEntity(location, true);
    }

    @Nullable
    public static ItemEntity getFurnitureEntity(Location location, boolean isCheckAdjacent) {
        var displayEntity = getDisplayEntity(location, isCheckAdjacent, CustomBlockManager::isFurnitureDisplay);
        return displayEntity == null ? null : new DisplayItemEntity(displayEntity);
    }

    @Nullable
    private static ItemDisplay getDisplayEntity(Location location, boolean isCheckAdjacent, java.util.function.Predicate<ItemDisplay> filter) {
        var display = (ItemDisplay) getEntityByClass(location, ItemDisplay.class, entity -> filter.test((ItemDisplay) entity));
        if (display != null) {
            return display;
        }
        if (isCheckAdjacent) {
            return location.getNearbyEntitiesByType(ItemDisplay.class, 5)
                    .stream()
                    .filter(entity -> Barriers.read(entity).stream().anyMatch(vector -> entity.getLocation().toBlockLocation().toVector().clone().add(vector).equals(location.toBlockLocation().toVector())))
                    .filter(filter)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public static final String POTTED_KEY = "GardensFurniturePotted";

    // the display of a custom plant sitting in a vanilla flower pot (it isn't placeable furniture)
    @Nullable
    public static ItemDisplay getPottedPlant(Block pot) {
        return getDisplayEntity(pot.getLocation(), false,
                display -> NBT.getPersistentData(display, nbt -> (boolean) nbt.hasTag(POTTED_KEY)));
    }

    // other plugins (pets, cosmetics, holograms) spawn item displays too - only ours hold a placeable item
    public static boolean isFurnitureDisplay(ItemDisplay display) {
        var item = new DisplayItemEntity(display).getItem();
        return item != null && GardensFurniture.itemRegistry.getCustomItem(item) instanceof CustomPlaceableItem;
    }

    @Nullable
    public static Interaction getInteractionEntity(Location location) {
        return (Interaction) getEntityByClass(location, Interaction.class);
    }

    @Nullable
    public static Entity getEntityByClass(Location location, Class<? extends Entity> entityClass) {
        return getEntityByClass(location, entityClass, entity -> true);
    }

    @Nullable
    public static Entity getEntityByClass(Location location, Class<? extends Entity> entityClass, java.util.function.Predicate<Entity> filter) {
        Collection<Entity> entities = location.getWorld().getNearbyEntities(location, 1, 1, 1);
        Block center = location.getBlock();

        for (Entity entity : entities) {
            if (entityClass.isInstance(entity) && filter.test(entity)) {
                if (entity.getLocation().getBlock().getLocation().equals(center.getLocation()))
                    return entity;
            }
        }

        return null;
    }

    public static void breakFurniture(Player player, Location location, boolean isBreakingSystem) {
       if (player != null && !ProtectionUtil.canBuild(location, player)) {
           return;
       }
        // Expose the breaking player as the furniture lifecycle cause (for Prism attribution).
        FurnitureStoreHandler.currentActor.set(player);
        try {
            breakFurnitureInternal(player, location, isBreakingSystem);
        } finally {
            FurnitureStoreHandler.currentActor.remove();
        }
    }

    private static void breakFurnitureInternal(Player player, Location location, boolean isBreakingSystem) {
        ItemEntity itemEntity = getFurnitureEntity(location);
        if (itemEntity == null) return;

        CustomItem customItem = GardensFurniture.itemRegistry.getCustomItem(itemEntity.getItem());
        if (!(customItem instanceof CustomPlaceableItem placeable)) {
            return; // not our furniture - never remove another plugin's entity
        }
        // TODO temp disabled since breaking system is disabled
//        if (player != null && player.getGameMode() != GameMode.CREATIVE && !isBreakingSystem && location.getBlock().getType() == Material.BARRIER && placeable != null && placeable.getCustomBlock().getMaterialTraits() != null) {
//            return; // breaks with breaking handler
//        }
        FurnitureBreakEvent e = new FurnitureBreakEvent(player, (CustomPlaceableItem) customItem, itemEntity);
        Bukkit.getPluginManager().callEvent(e);
        if (e.isCancelled()) {
            return;
        }

        // remove furniture entities / hitbox
        var barriers = Barriers.read(itemEntity.getEntity());
        if (!barriers.isEmpty()) {
            barriers.stream().map(v -> itemEntity.getEntity().getLocation().add(v).getBlock()).forEach(block -> {
                if (block.getType() == Material.BARRIER) {
                    block.setType(Material.AIR);
                }
            });
        }
        itemEntity.getEntity().remove();
        var interactionEntity = getInteractionEntity(location);
        if (interactionEntity != null) {
            interactionEntity.remove();
        }
        Block block = location.getBlock();
        if (block.getType() == Material.BARRIER) {
            block.setType(Material.AIR);
        }

        ItemStack itemStack = getDrop(itemEntity, placeable);
        if (player != null && player.getGameMode() != GameMode.CREATIVE && itemStack != null) {
            location.getWorld().dropItemNaturally(location.add(0.5, 0.1, 0.5), itemStack);
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
            Material.BIRCH_LEAVES,
            Material.SPRUCE_LEAVES,
            Material.CHERRY_LEAVES,
            Material.NOTE_BLOCK,
            Material.CAVE_VINES,
            Material.CAVE_VINES_PLANT,
            Material.TRIPWIRE,
            Material.OAK_SAPLING,
            Material.DARK_OAK_SAPLING
    };

    /**
     * Remove a store-backed custom block and everything that belongs to it, without dropping anything:
     * a model display in its cell (saplings), the other half of a door or large tripwire, its light, its
     * store, and the block itself. Callers handle drops, since those differ per cause.
     */
    public static void removeCustomBlock(Block block) {
        var customBlock = getItemFromLeaves(block) instanceof CustomPlaceableItem placeable ? placeable.getCustomBlock() : null;
        var itemEntity = getItemEntity(block.getLocation());
        if (itemEntity != null) {
            itemEntity.getEntity().remove();
        }
        Block partner = null;
        if (block.getBlockData() instanceof org.bukkit.block.data.type.Door door) {
            partner = door.getHalf() == org.bukkit.block.data.Bisected.Half.BOTTOM ? block.getRelative(BlockFace.UP) : block.getRelative(BlockFace.DOWN);
        } else if (customBlock instanceof CustomTripwireBlock tripwire && tripwire.isLarge()) {
            partner = block.getRelative(BlockFace.UP);
        }
        if (partner != null && FurnitureStoreHandler.INSTANCE.getFurnitureStore(partner) != null) {
            FurnitureStoreHandler.INSTANCE.removeFurnitureStore(partner);
            partner.setType(Material.AIR, false);
        }
        block.setType(Material.AIR);
        FurnitureStoreHandler.INSTANCE.removeFurnitureStore(block);
        if (customBlock != null) {
            customBlock.removeLight(block.getLocation());
        }
    }

    // every block type whose custom item lives in a furniture store rather than a display entity
    private static final Set<Material> STORE_BACKED_TYPES = Collections.unmodifiableSet(EnumSet.copyOf(
            Stream.concat(Arrays.stream(packetTypes), CustomWaxedBlock.WaxedType.materials().stream()).toList()
    ));

    public static boolean isStoreBacked(Material material) {
        return STORE_BACKED_TYPES.contains(material);
    }

    @Nullable
    public static CustomItem getCustomItemFromBlock(Block block) {
        var type = block.getType();
        if (isStoreBacked(type)) {
            return CustomBlockManager.getItemFromLeaves(block);
        }
        // Display furniture only ever occupies barrier cells or the free cell it was placed in, so skip the
        // entity lookup for everything else - this runs on every mob step, block place and block break.
        ItemEntity itemEntity;
        if (type == Material.BARRIER) {
            itemEntity = getFurnitureEntity(block.getLocation());
        } else if (type == Material.CAVE_AIR || isPlaceableMaterial(type)) {
            itemEntity = getFurnitureEntity(block.getLocation(), false); // interaction hitboxes: own cell only
        } else {
            return null;
        }
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
        var item = itemEntity.getItem();
        if (block instanceof CustomDisplayBlock displayBlock && (displayBlock.getModel_override() != null || displayBlock.isLightToggleable())) {
            // re-place original item model
            item.editMeta(meta -> meta.setItemModel(NamespacedKey.fromString(customPlaceableItem.getComponents().getItem_model())));
        }
        return item;
    }

    public static void onInteractFurniture(Player player, EquipmentSlot hand, ItemEntity itemEntity, Interaction interaction) {
        ItemStack heldItem = player.getInventory().getItem(hand);
        CustomItem customItem = GardensFurniture.itemRegistry.getCustomItem(itemEntity.getItem());
        if (!(customItem instanceof CustomPlaceableItem customBlockItem)) {
            return;
        }
        var event = new FurnitureInteractEvent(player, new Furniture(customItem, (ItemDisplay) itemEntity.getEntity(), interaction), hand, heldItem);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        var location = itemEntity.getEntity().getLocation();
        // using furniture (sitting, switching a light) needs access trust, like buttons and levers;
        // changing it (dyeing) or taking from it (maple taps) needs build trust
        boolean canUse = ProtectionUtil.canUse(location, player);
        boolean canBuild = ProtectionUtil.canBuild(location, player);
        if (!canUse && !canBuild) {
            return;
        }
        if (player.isSneaking()) {
//            itemEntity.rotate(45);
        } else if (heldItem.getType().toString().toUpperCase().endsWith("DYE") && customItem.isDyeable() && itemEntity.getItem().getItemMeta() instanceof LeatherArmorMeta leatherMeta) {
            if (!canBuild) return;
            leatherMeta.setColor(getColorFromDye(heldItem.getType()));
            var item = itemEntity.getItem();
            item.setItemMeta(leatherMeta);
            itemEntity.setItem(item);
            heldItem.setAmount(heldItem.getAmount() - 1);
        } else if (customBlockItem.getCustomBlock() instanceof CustomDisplayBlock displayBlock && displayBlock.isLightToggleable()) {
            // interact fires once per hand, only toggle on the main hand
            if (hand == EquipmentSlot.HAND) {
                displayBlock.toggleLight((ItemDisplay) itemEntity.getEntity());
            }
        } else if (customBlockItem.getCustomBlock().isSeated()) {
            double offset = customBlockItem.getCustomBlock().getSeatOffset();
            GSitAPI.createSeat(itemEntity.getEntity().getLocation().getBlock(), player, true, 0.0, offset, 0.0, player.getLocation().getYaw(), true);
        } else if (customItem.getIdentifier().equals("maple_tap_5") && heldItem.getType() == Material.GLASS_BOTTLE) {
            if (!canBuild) return;
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
