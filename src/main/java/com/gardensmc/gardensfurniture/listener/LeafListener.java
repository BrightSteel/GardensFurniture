package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.block.CustomLeavesBlock;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import com.gardensmc.gardensfurniture.util.CustomDropUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.LeavesDecayEvent;

import java.util.Arrays;

public class LeafListener implements Listener {

    @EventHandler
    public void leafDecay(LeavesDecayEvent e) {
        if (e.isCancelled()) return;

        Block leafBlock = e.getBlock();
        if (leafBlock.getBlockData() instanceof Leaves leaf) {
            var customItem = CustomBlockManager.getItemFromLeaves(leafBlock);
            if (!(customItem instanceof CustomPlaceableItem placeable)) return;
            if (!preventDecay(leafBlock, leaf)) {
                FurnitureStoreHandler.INSTANCE.removeFurnitureStore(e.getBlock());
                e.getBlock().setType(Material.AIR);
                CustomDropUtil.leafDrop(leafBlock.getLocation(), (CustomLeavesBlock) placeable.getCustomBlock());
            }
            e.setCancelled(true);
        }
    }

    private boolean preventDecay(Block leafBlock, Leaves leaf) {
        int distance = leaf.getDistance();
//        int distance = 14;
        for (int x = -distance; x < distance; x++) {
            for (int y = -distance; y < distance; y++) {
                for (int z = -distance; z < distance; z++) {
                    Block b = leafBlock.getRelative(x, y, z);
                    if (b.getType() == Material.NOTE_BLOCK) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
