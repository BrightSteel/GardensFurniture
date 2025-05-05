package com.gardensmc.gardensfurniture.loader;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.block.CustomCrystalBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomDisplayBlock;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.item.PlaceOptions;
import com.gardensmc.gardensfurniture.store.FurnitureCache;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.AmethystCluster;
import org.bukkit.block.data.type.NoteBlock;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class ChunkFurnitureLoader {

//    public static final Queue<FurnitureReplacement> furnitureReplacementQueue = new LinkedList<>();
    public static ChunkFurnitureLoader INSTANCE = new ChunkFurnitureLoader();

    public void processSnapshot(ChunkSnapshot chunkSnapshot) {
        boolean updateCache = false;
        for (int x = 0; x < 16; x++) {
            for (int y = 200; y > -60; y--) {
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
        if (mapping.equals("amethyst_budding")) {
            placeAmethystBuddingWithBuds(block);
        } else {
            CustomItem customItem = GardensFurniture.itemRegistry.getItem(mapping);
            if (customItem instanceof CustomPlaceableItem placeableItem) {
                block.setType(Material.AIR);
                placeableItem.placeBlock(null, null, block.getLocation(), new PlaceOptions().setPersistentLeaves(false).setForce(true));
            }
        }
    }

    // custom mapping for normal amethyst buds, but place it with chance of budding
    private void placeAmethystBuddingWithBuds(Block block) {
        block.setType(Material.BUDDING_AMETHYST);
        // chance of buds
        for (BlockFace face : List.of(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
            if (ThreadLocalRandom.current().nextInt(2) == 0) {
                var b = block.getRelative(face);
                if (b.getType() == Material.AIR) {
                    var budMaterial = CustomCrystalBlock.CrystalType.getRandom().getCrystalMaterial();
                    b.setType(budMaterial);
                    if (b.getBlockData() instanceof AmethystCluster cluster) {
                        cluster.setFacing(face);
                        b.setBlockData(cluster);
                    }
                    // normal amethyst - rotated to better fit display "fixed" setting
                    var amethystBudDupe = GardensFurniture.itemRegistry.getItem(budMaterial.toString().toLowerCase());
                    if (amethystBudDupe instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomDisplayBlock displayBlock) {
                        displayBlock.placeBlock(null, null, placeable, b.getLocation(), new PlaceOptions().setBlockFace(face).setPersistentLeaves(false));
                    }
                }
            }
        }
    }

    public record FurnitureReplacement(String world, int x, int y, int z, String mapping) {}
}
