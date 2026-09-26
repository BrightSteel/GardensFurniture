package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomBackpackItem;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomWrapItem;
import com.gardensmc.gardensfurniture.wrap.WrapHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.meta.LeatherArmorMeta;

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
        var color = wrap.getItemMeta() instanceof LeatherArmorMeta leatherMeta ? leatherMeta.getColor() : null;
        var wrapResult = new WrapHandler(tool).wrapItem(customWrapItem, color);
        e.setResult(wrapResult);
        e.getView().setRepairCost(3);
        e.getView().setRepairItemCountCost(1); // one wrap per item, not the whole stack
    }

}
