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
        if (customItem.getIdentifier().equals("tripwire_top")) {
            Block bottom = e.getBlock().getRelative(BlockFace.DOWN);
//            var furnitureStore = FurnitureStoreHandler.INSTANCE.getFurnitureStore(bottom);
//            var tripItem = GardensFurniture.itemRegistry.getItem(furnitureStore.itemIdentifier());
            // wait are we going to support multiplier big ones ?
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

            if (e.getPlayer().getGameMode() != GameMode.CREATIVE && !customBlockItem.getCustomBlock().cancelDrop()){
                ItemStack itemStack = GardensFurniture.itemRegistry.buildItem(customItem);
                if (itemStack != null) {
                    boolean drop;
                    if (shearable) {
                        ItemStack held = e.getPlayer().getInventory().getItemInMainHand();
                        drop = held.getType() == Material.SHEARS || held.containsEnchantment(Enchantment.SILK_TOUCH);
                    } else {
                        drop = true;
                    }
                    if (drop) {
                        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), itemStack);
                    }
                    else if (e.getBlock().getBlockData() instanceof Leaves leaves1 && customBlockItem.getCustomBlock() instanceof CustomLeavesBlock customLeavesBlock) {
                        if (!leaves1.isPersistent()) { // need 2 be non-persistent server-side to drop sapling
                            // chance of dropping sapling
                            if (ProbabilityUtil.roll(5)) {
                                String saplingId = customLeavesBlock.getSapling();
                                if (saplingId != null) {
                                    CustomItem saplingItem = GardensFurniture.itemRegistry.getItem(saplingId);
                                    if (saplingItem != null) {
                                        ItemStack sapling = GardensFurniture.itemRegistry.buildItem(saplingItem);
                                        if (sapling != null) {
                                            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), sapling);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Bukkit.getPluginManager().callEvent(new CustomBlockBreakEvent(e.getPlayer(), customBlockItem, e.getBlock()));
            if (removeFurniture) {
                FurnitureStoreHandler.INSTANCE.removeFurnitureStore(e.getBlock());
            }
        }
    }
}
