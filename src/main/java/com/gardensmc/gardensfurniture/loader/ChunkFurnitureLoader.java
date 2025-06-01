package com.gardensmc.gardensfurniture.loader;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.CustomDisplayBlock;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.item.PlaceOptions;
import com.gardensmc.gardensfurniture.store.FurnitureCache;
import com.gardensmc.gardensfurniture.structure.StructureHandler;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.AmethystCluster;
import org.bukkit.block.data.type.NoteBlock;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class ChunkFurnitureLoader {

//    public static final Queue<FurnitureReplacement> furnitureReplacementQueue = new LinkedList<>();
    public static ChunkFurnitureLoader INSTANCE = new ChunkFurnitureLoader();

    public void processSnapshot(ChunkSnapshot chunkSnapshot) {
        boolean updateCache = false;
        for (int x = 0; x < 16; x++) {
            for (int y = 230; y > -60; y--) {
                for (int z = 0; z < 16; z++) {
                    BlockData blockData = chunkSnapshot.getBlockData(x, y, z);
                    if (blockData instanceof NoteBlock noteBlock) {
                        String key = String.format(
                                "[%s, %s, %s]",
                                noteBlock.getNote().getId(),
                                noteBlock.getInstrument().toString().toLowerCase(),
                                noteBlock.isPowered()

                        );
                        String mapping = ConfigUtil.generationMappings.getString(key);
                        if (mapping != null) {
                            int finalX = x;
                            int finalY = y;
                            int finalZ = z;
                            Bukkit.getScheduler().runTask(GardensFurniture.plugin, () -> placeFurnitureBlock(chunkSnapshot.getWorldName(), chunkSnapshot.getX(), chunkSnapshot.getZ(), finalX, finalY, finalZ, mapping));
                            updateCache = true;
//                            furnitureReplacementQueue.add(new FurnitureReplacement(chunkSnapshot.getWorldName(), x, y, z, mapping));
                        }
                    }
                }
            }
        }

        // limit cache update to once per chunk
        if (updateCache) {
            Bukkit.getScheduler().runTask(GardensFurniture.plugin, () -> FurnitureCache.INSTANCE.refreshChunk(new FurnitureCache.ChunkKey(chunkSnapshot.getWorldName(), chunkSnapshot.getX(),chunkSnapshot.getZ())));
        }
    }

    private void placeFurnitureBlock(String world, int chunkX, int chunkZ, int x, int y, int z, String mapping) {
        var bukkitWorld = Bukkit.getWorld(world);
        var chunk = Objects.requireNonNull(bukkitWorld).getChunkAt(chunkX, chunkZ);
        Block block = chunk.getBlock(x, y, z);
        // custom mappings
        CustomItem customItem = GardensFurniture.itemRegistry.getItem(mapping);
        block.setType(Material.AIR);
        if (customItem instanceof CustomPlaceableItem placeableItem) {
            placeableItem.placeBlock(null, null, block.getLocation(), new PlaceOptions().setPersistentLeaves(false));
        } else {
            if (!StructureHandler.INSTANCE.spawnStructure(mapping, block.getLocation())) {
                GardensFurniture.plugin.getLogger().log(Level.SEVERE, "Generation mapping " + mapping + " is not a placeable custom block or structure!");
            }
        }
    }

    public record FurnitureReplacement(String world, int x, int y, int z, String mapping) {}
}
