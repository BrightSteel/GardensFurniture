package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomBackpackItem;
import com.gardensmc.gardensfurniture.wrap.WrapHandler;
import com.gardensmc.gardensfurniture.util.SpecialItems;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.ShulkerBox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

public class CraftListener implements Listener {

    private static final Material[] BACKPACK_RECIPE = new Material[] {
            null, Material.LEATHER, null,
            Material.LEATHER, Material.SHULKER_BOX, Material.LEATHER,
            null, Material.LEATHER, null
    };

    @EventHandler
    public void onCraftBackpack(PrepareItemCraftEvent e) {
        var currResult = e.getInventory().getResult();
        if (currResult != null && currResult.getType() != Material.AIR) {
            return;
        }
        var matrix = e.getInventory().getMatrix();
        if (IntStream.range(0, 9).allMatch(i -> isEqual(matrix[i], BACKPACK_RECIPE[i]))) {
            var shulker = matrix[4];
            if (shulker.getItemMeta() instanceof BlockStateMeta blockStateMeta) {
                if (blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
                    var dye = shulkerBox.getColor() == null ? "" : "_" + shulkerBox.getColor().toString().toLowerCase();
                    var backpack = GardensFurniture.itemRegistry.buildItem(SpecialItems.BACKPACK_PREFIX + dye);
                    // a color with no backpack of its own still makes one, the plain one, rather than nothing
                    if (backpack == null) backpack = GardensFurniture.itemRegistry.buildItem(SpecialItems.BACKPACK_PREFIX);
                    if (backpack != null) {
                        backpack.setData(DataComponentTypes.CONTAINER, shulker.getData(DataComponentTypes.CONTAINER));
                        e.getInventory().setResult(backpack);
                    }
                }
            }
        }
    }

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
        if (new WrapHandler(currResult).getWrapFromItem() != null) {
            return; // ignore wrapped backpacks
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

    private boolean isEqual(ItemStack stack, Material material) {
        if (stack == null || stack.getType() == Material.AIR) {
            return material == null;
        }
        if (stack.getAmount() != 1) {
            return false;
        }
        if (material == Material.SHULKER_BOX) {
            return stack.getType().toString().toLowerCase().endsWith("shulker_box");
        }
        return stack.getType() == material;
    }
}
