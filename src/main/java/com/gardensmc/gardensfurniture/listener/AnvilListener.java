package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.backpack.BackpackHandler;
import com.gardensmc.gardensfurniture.custom.item.CustomBackpackItem;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomWrapItem;
import com.gardensmc.gardensfurniture.wrap.WrapHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;

public class AnvilListener implements Listener {

    @EventHandler
    public void onWrapItem(PrepareAnvilEvent e) {
        var tool = e.getInventory().getFirstItem();
        var wrap = e.getInventory().getSecondItem();
        if (tool == null || wrap == null) {
            return;
        }
        var wrapItem = GardensFurniture.itemRegistry.getCustomItem(wrap);
        if (!(wrapItem instanceof CustomWrapItem customWrapItem)) {
            return;
        }
        var isWrappable = customWrapItem.getWrapType().isWrappable.test(tool);
        if (!isWrappable) {
            return;
        }
        var wrapResult = new WrapHandler(tool).wrapItem(customWrapItem);
        e.setResult(wrapResult);
        e.getView().setRepairCost(3);
    }

    @EventHandler
    public void onSlotRuneBackpack(PrepareAnvilEvent e) {
        var backpack = e.getInventory().getFirstItem();
        var rune = e.getInventory().getSecondItem();
        if (backpack == null || rune == null) {
            return;
        }
        var backpackItem = GardensFurniture.itemRegistry.getCustomItem(backpack);
        if (!(backpackItem instanceof CustomBackpackItem customBackpackItem)) {
            return;
        }
        var runeItem = GardensFurniture.itemRegistry.getCustomItem(rune);
        if (!(runeItem instanceof CustomItem customItem) || !customItem.getIdentifier().equals("expansion_rune")) {
            return;
        }
        var result = new BackpackHandler(backpack, customBackpackItem).getUpgradeRowsResult();
        if (result != null) {
            e.setResult(result);
            e.getView().setRepairCost(3);
        }
    }
}
