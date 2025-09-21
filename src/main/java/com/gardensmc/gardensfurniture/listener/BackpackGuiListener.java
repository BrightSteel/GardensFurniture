package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.backpack.BackpackHandler;
import com.gardensmc.gardensfurniture.custom.item.CustomBackpackItem;
import com.gardensmc.gardensfurniture.menu.BackpackGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class BackpackGuiListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        var clickedInv = e.getClickedInventory();
        if (clickedInv != null && clickedInv.getHolder() instanceof BackpackGui backpackGui) {
            if (e.getSlot() < backpackGui.getFillerSlots()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player && e.getInventory().getHolder() instanceof BackpackGui backpackGui) {
            if (e.getRawSlots().stream().anyMatch(slot -> slot < backpackGui.getFillerSlots())) {
                e.setCancelled(true);
            }
        }
    }

    // does this do anything? Had to add the shiftInto and dropInto events to actually prevent placing
    @EventHandler(ignoreCancelled = true)
    public void onPlaceItemIntoBackpack(InventoryMoveItemEvent e) {
        if (e.getDestination().getHolder() instanceof BackpackGui) {
            if (!isValidItem(e.getItem())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onShiftIntoBackpack(InventoryClickEvent e) {
        // shifting from player inv -> backpack
        if (e.getClickedInventory() instanceof PlayerInventory) {
            if (e.getView().getTopInventory().getHolder() instanceof BackpackGui) {
                if (e.getClick() == ClickType.SHIFT_LEFT) {
                    var item = e.getClickedInventory().getItem(e.getSlot());
                    if (!isValidItem(item)) {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDropIntoBackpack(InventoryClickEvent e) {
        var clickedInv = e.getClickedInventory();
        if (clickedInv != null && clickedInv.getHolder() instanceof BackpackGui) {
            if (e.getClick() == ClickType.LEFT) {
                if (!isValidItem(e.getCursor())) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDragItemsIntoBackpack(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof BackpackGui) {
            if (!e.getNewItems().values().stream().allMatch(this::isValidItem)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof BackpackGui backpackGui) {
            BackpackHandler.onClose((Player) e.getPlayer(), backpackGui.getBackpackItem(), backpackGui.getItemContents());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMoveBackpackInInventory(InventoryClickEvent e) {
        if (e.getClickedInventory() instanceof PlayerInventory playerInventory) {
            var item = e.getClickedInventory().getItem(e.getSlot());
            if (BackpackHandler.isOpenBackpack(playerInventory, item)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemChangeInventory(InventoryMoveItemEvent e) {
        if (e.getSource() instanceof PlayerInventory playerInventory) {
            var item = e.getItem();
            Player player = (Player) playerInventory.getHolder();
            handleBackpackLeaveInventory(item, player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFillerChangeInventory(InventoryMoveItemEvent e) {
        if (e.getSource().getHolder() instanceof BackpackGui) {
            var customItem = GardensFurniture.itemRegistry.getCustomItem(e.getItem());
            if (customItem != null && customItem.getIdentifier().equals("blank_hidden")) {
                // prevent u from pressing q to drop blank items
                e.setCancelled(true);
            }
        }
    }

    private void handleBackpackLeaveInventory(ItemStack potentialBackpack, Player player) {
        if (GardensFurniture.itemRegistry.getCustomItem(potentialBackpack) instanceof CustomBackpackItem) {
            BackpackHandler.closeBackpackIfOpen(player, potentialBackpack);
        }
    }

    private boolean isValidItem(ItemStack item) {
        if (item != null) {
            var typeStr = item.getType().toString().toLowerCase();
            if (typeStr.endsWith("shulker_box") || typeStr.endsWith("bundle")) {
                return false;
            }
            return !(GardensFurniture.itemRegistry.getCustomItem(item) instanceof CustomBackpackItem);
        }
        return true;
    }
}
