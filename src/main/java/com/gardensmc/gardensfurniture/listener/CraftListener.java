package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.util.ItemUtil;
import com.gardensmc.gardensfurniture.custom.item.CustomBackpackItem;
import com.gardensmc.gardensfurniture.util.SpecialItems;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class CraftListener implements Listener {

    @EventHandler
    public void onDyeBackpack(PrepareItemCraftEvent e) {
        var currResult = e.getInventory().getResult();
        if (currResult == null) {
            return;
        }
        var items = Arrays.stream(e.getInventory().getMatrix()).filter(itemStack -> itemStack != null && itemStack.getType() != Material.AIR).toList();
        if (items.size() != 2) {
            return;
        }
        ItemStack dye = null;
        ItemStack backpack = null;
        if (isDye(items.getFirst())) {
            dye = items.getFirst();
            if (isBackpack(items.get(1))) {
                backpack = items.get(1);
            }
        } else if (isDye(items.get(1))) {
            dye = items.get(1);
            if (isBackpack(items.getFirst())) {
                backpack = items.getFirst();
            }
        }
        if (dye == null || backpack == null) {
            return;
        }
        if (ItemUtil.isSkinned(backpack)) {
            return; // a skinned backpack is just dyed: its look is the skin's
        }
        // modify item model
        var backpackClone = backpack.clone();
        // the dye's whole color, "light_blue" from LIGHT_BLUE_DYE (cut at the first underscore it was "light", and
        // light blue and light gray dyed nothing)
        var color = dye.getType().toString().toLowerCase().replaceFirst("_dye$", "");
        var desiredBackpack = GardensFurniture.itemRegistry.getItem(SpecialItems.BACKPACK_PREFIX + "_" + color);
        if (desiredBackpack == null) {
            return;
        }
        backpackClone.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.fromString(desiredBackpack.getComponents().getItem_model()));
        e.getInventory().setResult(backpackClone);
    }

    private boolean isDye(ItemStack itemStack) {
        return itemStack.getType().toString().toLowerCase().endsWith("_dye");
    }

    private boolean isBackpack(ItemStack itemStack) {
        return GardensFurniture.itemRegistry.getCustomItem(itemStack) instanceof CustomBackpackItem;
    }
}
