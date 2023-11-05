package com.gardensmc.gardensfurniture.listener;

import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
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
                for (int y = -45; y < 200; y++) {
                    for (int z = 0; z < 16; z++) {
                        Block block = e.getChunk().getBlock(x, y, z);
                        if (block.getBlockData() instanceof NoteBlock noteBlock) {
                            if (noteBlock.getInstrument() == Instrument.BANJO) {
                                String identifier = null;
                                switch (noteBlock.getNote().getId()) {
                                    case 2 -> identifier = "maple_leaves_green";
                                    case 3 -> identifier = "maple_leaves_red";
                                    case 4 -> identifier = "maple_leaves_gold";
                                    case 5 -> identifier = "maple_leaves_orange";
                                    case 6 -> identifier = "palm_leaves";
                                    case 7 -> identifier = "redwood_leaves";
                                }

                                if (identifier != null) {
                                    block.setType(Material.SPRUCE_LEAVES);
                                    if (block.getBlockData() instanceof Leaves leaves) {
                                        leaves.setDistance(7);
                                        leaves.setPersistent(false);
                                    }
                                    e.getChunk().getPersistentDataContainer().set(new NamespacedKey("gardens", block.getX() + "." + block.getY() + "." + block.getZ()), PersistentDataType.STRING, identifier);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
