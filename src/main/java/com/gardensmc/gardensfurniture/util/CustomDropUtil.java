package com.gardensmc.gardensfurniture.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.*;
import com.gardensmc.gardensfurniture.custom.block.CustomLeavesBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomTripwireBlock;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomMultiplierItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.events.CustomBlockBreakEvent;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class CustomDropUtil {

    // I guess this is a custom handler for tripwire? Why is it hidden in this util, there should be a factory or something
    public static void dropCustomItem(BlockBreakEvent e, CustomItem customItem, CustomTripwireBlock customTripwireBlock) {
        e.setDropItems(false);
        if (customItem.getIdentifier().equals(SpecialItems.TRIPWIRE_TOP)) {
            Block bottom = e.getBlock().getRelative(BlockFace.DOWN);
            CustomItem tripItem = CustomBlockManager.getItemFromLeaves(bottom);

            // remove top from store
            FurnitureStoreHandler.INSTANCE.removeFurnitureStore(e.getBlock());
            if (tripItem != null) {
                // drop item
                ItemStack drop = GardensFurniture.itemRegistry.buildItem(tripItem);
                bottom.setType(Material.AIR);
                if (drop != null) {
                    e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), drop);
                }
                // remove bottom from store
                FurnitureStoreHandler.INSTANCE.removeFurnitureStore(bottom);
            }
        }
        else {
            if (customTripwireBlock.isLarge()) {
                // remove top component
                Block top = e.getBlock().getRelative(BlockFace.UP);
                top.setType(Material.AIR);
                FurnitureStoreHandler.INSTANCE.removeFurnitureStore(top);
            }

            ItemStack drop = GardensFurniture.itemRegistry.buildItem(customItem);
            int amountToDrop = 1;
            if (customItem instanceof CustomMultiplierItem) {
                // bottom's store
                var furnitureStore = FurnitureStoreHandler.INSTANCE.getFurnitureStore(e.getBlock());
                amountToDrop = furnitureStore.getMultiplier();
            }
            if (drop != null) {
                drop.setAmount(amountToDrop);
                e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), drop);
            }
            // remove bottom from store
            FurnitureStoreHandler.INSTANCE.removeFurnitureStore(e.getBlock());
        }
    }

    public static void dropCustomItem(BlockBreakEvent e, CustomItem customItem, boolean removeFurniture, boolean shearable) {
        if (customItem instanceof CustomPlaceableItem customBlockItem) {
            e.setDropItems(false);

            if (e.getPlayer().getGameMode() != GameMode.CREATIVE && !customBlockItem.getCustomBlock().cancelDrop()) {
                var dropOverride = customBlockItem.getCustomBlock().getDropOverride();
                ItemStack itemStack = dropOverride != null
                        ? GardensFurniture.itemRegistry.buildItem(dropOverride)
                        : GardensFurniture.itemRegistry.buildItem(customItem);
                if (itemStack != null) {
                    boolean drop;
                    if (shearable) {
                        ItemStack held = e.getPlayer().getInventory().getItemInMainHand();
                        drop = held.getType() == Material.SHEARS || held.containsEnchantment(Enchantment.SILK_TOUCH);
                    } else {
                        drop = true;
                    }
                    if (drop) {
                        // drop after block is removed to avoid it colliding and gaining velocity
                        Bukkit.getScheduler().runTaskLater(GardensFurniture.plugin, () -> e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation().add(0.5, 0.1, 0.5), itemStack), 1);
                    }
                    else if (e.getBlock().getBlockData() instanceof Leaves leaves1 && customBlockItem.getCustomBlock() instanceof CustomLeavesBlock customLeavesBlock) {
                        if (!leaves1.isPersistent()) { // need 2 be non-persistent server-side to drop sapling
                            Bukkit.getScheduler().runTaskLater(GardensFurniture.plugin, () -> leafDrop(e.getBlock().getLocation().add(0.5, 0.1, 0.5), customLeavesBlock), 1);
                        }
                    }
                }
            }

            Bukkit.getPluginManager().callEvent(new CustomBlockBreakEvent(e.getPlayer(), customBlockItem, e.getBlock()));
            if (removeFurniture) {
                // run later for other listeners to see furniture data before removal; carry the breaking
                // player and the pre-break block state so the removal is recorded against them (Prism)
                var player = e.getPlayer();
                var brokenBlockData = e.getBlock().getBlockData().getAsString();
                Bukkit.getScheduler().runTaskLater(GardensFurniture.plugin, () -> {
                    FurnitureStoreHandler.currentActor.set(player);
                    FurnitureStoreHandler.removedBlockData.set(brokenBlockData);
                    try {
                        FurnitureStoreHandler.INSTANCE.removeFurnitureStore(e.getBlock());
                    } finally {
                        FurnitureStoreHandler.currentActor.remove();
                        FurnitureStoreHandler.removedBlockData.remove();
                    }
                }, 1);
            }
        }
    }

    public static void leafDrop(Location location, CustomLeavesBlock customLeavesBlock) {
        if (ProbabilityUtil.roll(5)) {
            String saplingId = customLeavesBlock.getSapling();
            if (saplingId != null) {
                CustomItem saplingItem = GardensFurniture.itemRegistry.getItem(saplingId);
                if (saplingItem != null) {
                    ItemStack sapling = GardensFurniture.itemRegistry.buildItem(saplingItem);
                    if (sapling != null) {
                        location.getWorld().dropItemNaturally(location, sapling);
                    }
                }
            }
        }
        if (ProbabilityUtil.roll(2)) {
            location.getWorld().dropItem(location, new ItemStack(Material.STICK));
        }
    }
}
