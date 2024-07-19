package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.block.CustomLeavesBlock;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;

public class ChunkListener implements Listener {

    @EventHandler
    public void onLoadChunk(ChunkLoadEvent e) {
        if (e.isNewChunk()) {
            for (int x = 0; x < 16; x++) {
                for (int y = 200; y > -60; y--) {
                    for (int z = 0; z < 16; z++) {
                        Block block = e.getChunk().getBlock(x, y, z);
                        if (block.getBlockData() instanceof NoteBlock noteBlock) {
                            String key = String.format(
                                    "[%s, %s, %s]",
                                    noteBlock.getNote().getId(),
                                    noteBlock.getInstrument().toString().toLowerCase(),
                                    noteBlock.isPowered()

                            );
                            String mapping = ConfigUtil.generationMappings.getString(key);
                            if (mapping != null) {
                                CustomItem customItem = GardensFurniture.itemRegistry.getItem(mapping);
                                if (customItem instanceof CustomPlaceableItem placeableItem) {
                                    block.setType(Material.AIR);
                                    CustomBlockManager.attemptPlacement(null,null, placeableItem, block.getLocation(), null, false);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
