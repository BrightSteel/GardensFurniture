package com.gardensmc.gardensfurniture.recipe;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.util.ItemUtil;
import com.gardensmc.gardensfurniture.wrap.WrapHandler;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

/**
 * Furnaces compare whole items, so a GardensFurniture ore or log made before its yml last changed (an
 * older name, lore or model) won't smelt. When one goes into a furnace's input slot it's swapped for a
 * current copy with the same id and amount: what it would be if it were made today.
 *
 * <p>Only items one of our furnace recipes takes are touched, and never ones carrying something a
 * fresh copy wouldn't have: contents, a dye color or a wrap.
 */
public class FurnaceInputRefresher implements Listener {

    private static final int INPUT_SLOT = 0;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory() instanceof FurnaceInventory furnace) later(furnace);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory() instanceof FurnaceInventory furnace) later(furnace);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onOpen(InventoryOpenEvent e) {
        if (e.getInventory() instanceof FurnaceInventory furnace) refresh(furnace);
    }

    // a hopper above a furnace fills its input slot
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent e) {
        if (!(e.getDestination() instanceof FurnaceInventory)) return;
        var fresh = refreshed(e.getItem());
        if (fresh != null) e.setItem(fresh);
    }

    // after the click has landed
    private static void later(FurnaceInventory furnace) {
        Bukkit.getScheduler().runTask(GardensFurniture.plugin, () -> refresh(furnace));
    }

    private static void refresh(FurnaceInventory furnace) {
        var fresh = refreshed(furnace.getItem(INPUT_SLOT));
        if (fresh != null) furnace.setItem(INPUT_SLOT, fresh);
    }

    /** A current copy of the item, or null when it's already current or shouldn't be touched. */
    @Nullable
    private static ItemStack refreshed(@Nullable ItemStack stack) {
        var key = RecipeManager.keyOf(stack);
        if (key == null || !key.isCustom() || !RecipeManager.INSTANCE.isCookingInput(key.customId())) return null;
        if (stack.hasData(DataComponentTypes.CONTAINER) || stack.hasData(DataComponentTypes.DYED_COLOR)
                || new WrapHandler(stack).getWrapFromItem() != null) {
            return null;
        }
        var fresh = GardensFurniture.itemRegistry.buildItem(key.customId());
        if (ItemUtil.isEmpty(fresh) || fresh.isSimilar(stack)) return null;
        fresh.setAmount(stack.getAmount());
        return fresh;
    }
}
