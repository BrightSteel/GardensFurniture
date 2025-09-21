package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.backpack.BackpackHandler;
import com.gardensmc.gardensfurniture.custom.item.CustomBackpackItem;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

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
                    var backpack = GardensFurniture.itemRegistry.buildItem("backpack" + dye);
                    if (backpack != null) {
                        backpack.setData(DataComponentTypes.CONTAINER, shulker.getData(DataComponentTypes.CONTAINER));
                        e.getInventory().setResult(backpack);
                    }
                }
            }
        }
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
