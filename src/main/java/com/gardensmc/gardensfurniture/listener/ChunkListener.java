package com.gardensmc.gardensfurniture.listener;

import com.comphenix.protocol.events.PacketEvent;
import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.*;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.loader.ChunkFurnitureLoader;
import com.gardensmc.gardensfurniture.store.FurnitureCache;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ChunkListener implements Listener {

    @EventHandler
    public void onLoadChunk(ChunkLoadEvent e) {
        var environment = e.getWorld().getEnvironment();
        if (environment != World.Environment.NETHER) {
            if (e.isNewChunk()) {
                var snapshot = e.getChunk().getChunkSnapshot();
                Bukkit.getScheduler().runTaskAsynchronously(
                        GardensFurniture.plugin,
                        () -> ChunkFurnitureLoader.INSTANCE.processSnapshot(snapshot, environment)
                );
            }
        }
    }

    // fix leaves
//    @EventHandler
//    public void onLeafLoad(ChunkLoadEvent e) {
//        if (!e.isNewChunk() && e.getWorld().getEnvironment() == World.Environment.NORMAL) {
//            var snapshot = e.getChunk().getChunkSnapshot();
//            Bukkit.getScheduler().runTaskAsynchronously(GardensFurniture.plugin, () -> processLeafFixer(snapshot));
//        }
//    }

    private void processLeafFixer(ChunkSnapshot chunkSnapshot) {
        for (int x = 0; x < 16; x++) {
            for (int y = 230; y > -60; y--) {
                for (int z = 0; z < 16; z++) {
                    BlockData blockData = chunkSnapshot.getBlockData(x, y, z);
                    if (blockData instanceof Leaves leaves) {
                        if (leaves.isPersistent()) {
                            int finalX1 = x;
                            int finalY1 = y;
                            int finalZ1 = z;
                            Bukkit.getScheduler().runTask(GardensFurniture.plugin, () -> {
                                var bukkitWorld = Bukkit.getWorld(chunkSnapshot.getWorldName());
                                var chunk = Objects.requireNonNull(bukkitWorld).getChunkAt(chunkSnapshot.getX(), chunkSnapshot.getZ());
                                var block = chunk.getBlock(finalX1, finalY1, finalZ1);
                                if (block.getBlockData() instanceof Leaves l) {
                                    l.setPersistent(false);
                                    block.setBlockData(l);
                                }
                            });
                        }
                    }
                }
            }
        }
    }
}
