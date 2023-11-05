package com.gardensmc.gardensfurniture.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.*;
import com.gardensmc.gardensfurniture.events.CustomBlockBreakEvent;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
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

    public static void dropCustomItem(BlockBreakEvent e, CustomItem customItem, CustomTripwireBlock customTripwireBlock) {
        e.setDropItems(false);
        if (customItem.getIdentifier().equals("tripwire_top")) {
            Block bottom = e.getBlock().getRelative(BlockFace.DOWN);
            CustomItem tripItem = CustomBlockManager.getItemFromLeaves(bottom);

            // remove top from store
            FurnitureStore.INSTANCE.removeFurnitureStore(e.getBlock());
            if (tripItem != null) {
                // drop item
                ItemStack drop = GardensFurniture.customRegister.itemRegistry.buildItem(tripItem);
                bottom.setType(Material.AIR);
                if (drop != null) {
                    e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), drop);
                }
                // remove bottom from store
                FurnitureStore.INSTANCE.removeFurnitureStore(bottom);
            }
        }
        else {
            if (customTripwireBlock.isLarge()) {
                // remove top component
                Block top = e.getBlock().getRelative(BlockFace.UP);
                top.setType(Material.AIR);
                FurnitureStore.INSTANCE.removeFurnitureStore(top);
            }

            ItemStack drop = GardensFurniture.customRegister.itemRegistry.buildItem(customItem);
            if (drop != null) {
                e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), drop);
            }
            // remove bottom from store
            FurnitureStore.INSTANCE.removeFurnitureStore(e.getBlock());
        }
    }

    public static void dropCustomItem(BlockBreakEvent e, CustomItem customItem, boolean leaves) {
        if (customItem != null) {
            boolean dropAnyway = customItem instanceof CustomCaveVineDirectional;
            e.setDropItems(false);

            if (e.getPlayer().getGameMode() != GameMode.CREATIVE && !customItem.getCustomBlock().cancelDrop()){
                ItemStack itemStack = GardensFurniture.customRegister.itemRegistry.buildItem(customItem);
                if (itemStack != null) {
                    boolean drop;
                    if (leaves) {
                        ItemStack held = e.getPlayer().getInventory().getItemInMainHand();
                        drop = held.getType() == Material.SHEARS || held.containsEnchantment(Enchantment.SILK_TOUCH);
                    } else {
                        drop = true;
                    }
                    if (drop || dropAnyway) {
                        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), itemStack);
                    }
                    else if (e.getBlock().getBlockData() instanceof Leaves leaves1 && customItem.getCustomBlock() instanceof CustomLeavesBlock customLeavesBlock) {
                        if (!leaves1.isPersistent()) { // need 2 be non-persistent server-side to drop sapling
                            // chance of dropping sapling
                            if (ProbabilityUtil.roll(5)) {
                                String saplingId = customLeavesBlock.getSapling();
                                if (saplingId != null) {
                                    CustomItem saplingItem = GardensFurniture.customRegister.itemRegistry.getItem(saplingId);
                                    if (saplingItem != null) {
                                        ItemStack sapling = GardensFurniture.customRegister.itemRegistry.buildItem(saplingItem);
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

            Bukkit.getPluginManager().callEvent(new CustomBlockBreakEvent(e.getPlayer(), customItem, e.getBlock()));
            if (leaves) {
                try {
                    FurnitureStore.INSTANCE.removeFurnitureStore(e.getBlock());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
