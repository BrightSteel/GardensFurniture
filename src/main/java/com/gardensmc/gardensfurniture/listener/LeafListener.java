package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.LeavesDecayEvent;

public class LeafListener implements Listener {

    @EventHandler
    public void neverLeafDecay(LeavesDecayEvent e) {
        if (e.isCancelled()) return;
        e.setCancelled(true);
    }

    // Commenting out since just cancelling all decay for now
//    @EventHandler
//    public void leafDecay(LeavesDecayEvent e) {
//        if (e.isCancelled()) return;
//
//        Block leafBlock = e.getBlock();
//        if (leafBlock.getBlockData() instanceof Leaves leaf && leafBlock.getType() == Material.SPRUCE_LEAVES) {
//            CustomItem customItem = CustomBlockManager.getItemFromLeaves(leafBlock);
//            if (customItem == null) return;
//            if (preventDecay(leafBlock, leaf)) {
//                e.setCancelled(true);
//            }
//        }
//    }

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
