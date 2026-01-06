package com.gardensmc.gardensfurniture.listener;

import com.destroystokyo.paper.block.BlockSoundGroup;
import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.loader.ChunkFurnitureLoader;
import com.gardensmc.gardensfurniture.store.FurnitureCache;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import com.gardensmc.gardensfurniture.util.BiomeUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.tr7zw.nbtapi.NBTChunk;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.util.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ChunkListener implements Listener {

    private static final String CHUNK_FIXER_KEY = "GardensChunkFixer";
    private static final int CHUNK_FIXER_VERSION = 1;
    private static final boolean ENABLE_CHUNK_FIXER = false; // change to true after incrementing version

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

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        if (ENABLE_CHUNK_FIXER) {
            if (!e.isNewChunk() && e.getWorld().getEnvironment() == World.Environment.NORMAL && GardensFurniture.coreProtectAPI != null) {
                var chunkNBT = new NBTChunk(e.getChunk()).getPersistentDataContainer();
                if (!Objects.equals(chunkNBT.getInteger(CHUNK_FIXER_KEY), CHUNK_FIXER_VERSION)) {
                    chunkNBT.setInteger(CHUNK_FIXER_KEY, CHUNK_FIXER_VERSION);
                    var snapshot = e.getChunk().getChunkSnapshot();
                    Bukkit.getScheduler().runTaskAsynchronously(GardensFurniture.plugin, () -> worldGenFixer(e.getWorld(), snapshot));
                }
            }
        }
    }

    private void worldGenFixer(World world, ChunkSnapshot chunkSnapshot) {
        for (int x = 0; x < 16; x++) {
            for (int y = 230; y > -60; y--) {
                for (int z = 0; z < 16; z++) {
                    BlockData blockData = chunkSnapshot.getBlockData(x, y, z);
                    if (blockData instanceof NoteBlock || blockData instanceof Leaves) {
                        int realX = chunkSnapshot.getX() * 16 + x;
                        int realZ = chunkSnapshot.getZ() * 16 + z;
                        var lookup = GardensFurniture.coreProtectAPI.blockLookup(toWrappedBlockData(blockData, realX, y, realZ, world), Integer.MAX_VALUE);
                        if (lookup.isEmpty()) {
                            int finalY = y;
                            if (blockData instanceof NoteBlock) {
                                Bukkit.getScheduler().runTask(GardensFurniture.plugin, () -> {
                                    var block = world.getBlockAt(realX, finalY, realZ);
                                    if (BiomeUtil.getBiomeKey(block.getLocation()).contains("palm_beach")) {
                                        var customItem = CustomBlockManager.getItemFromLeaves(block);
                                        if (customItem != null && customItem.getIdentifier().equals("eucalyptus_log")) {
                                            // modify furniture store
                                            var store = FurnitureStoreHandler.INSTANCE.getFurnitureStore(block);
                                            if (store != null) {
                                                FurnitureStoreHandler.INSTANCE.addFurnitureStore(
                                                        block,
                                                        new FurnitureStore("palm_log", store.getMultiplier(), store.getFacing()),
                                                        false
                                                );
                                            }
                                        }
                                    }
                                });
                            }
                            if (blockData instanceof Leaves leaves && leaves.isPersistent()) {
                                Bukkit.getScheduler().runTask(GardensFurniture.plugin, () -> {
                                    var block = world.getBlockAt(realX, finalY, realZ);
                                    if (block.getBlockData() instanceof Leaves l1) {
                                        l1.setPersistent(false);
                                        block.setBlockData(l1);
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }
    }

    private Block toWrappedBlockData(BlockData blockData, int x, int y, int z, World world) {
        return new Block() {

            @Override
            public byte getData() {
                return 0;
            }

            @Override
            public @NotNull BlockData getBlockData() {
                return blockData;
            }

            @Override
            public @NotNull Block getRelative(int i, int i1, int i2) {
                return null;
            }

            @Override
            public @NotNull Block getRelative(@NotNull BlockFace blockFace) {
                return null;
            }

            @Override
            public @NotNull Block getRelative(@NotNull BlockFace blockFace, int i) {
                return null;
            }

            @Override
            public @NotNull Material getType() {
                return null;
            }

            @Override
            public byte getLightLevel() {
                return 0;
            }

            @Override
            public byte getLightFromSky() {
                return 0;
            }

            @Override
            public byte getLightFromBlocks() {
                return 0;
            }

            @Override
            public @NotNull World getWorld() {
                return world;
            }

            @Override
            public int getX() {
                return x;
            }

            @Override
            public int getY() {
                return y;
            }

            @Override
            public int getZ() {
                return z;
            }

            @Override
            public boolean isValidTool(@NotNull ItemStack itemStack) {
                return false;
            }

            @Override
            public @NotNull Location getLocation() {
                return null;
            }

            @Override
            public @Nullable Location getLocation(@Nullable Location location) {
                return null;
            }

            @Override
            public @NotNull Chunk getChunk() {
                return null;
            }

            @Override
            public void setBlockData(@NotNull BlockData blockData) {

            }

            @Override
            public void setBlockData(@NotNull BlockData blockData, boolean b) {

            }

            @Override
            public void setType(@NotNull Material material) {

            }

            @Override
            public void setType(@NotNull Material material, boolean b) {

            }

            @Override
            public @Nullable BlockFace getFace(@NotNull Block block) {
                return null;
            }

            @Override
            public @NotNull BlockState getState() {
                return null;
            }

            @Override
            public @NotNull BlockState getState(boolean b) {
                return null;
            }

            @Override
            public @NotNull Biome getBiome() {
                return null;
            }

            @Override
            public @NotNull Biome getComputedBiome() {
                return null;
            }

            @Override
            public void setBiome(@NotNull Biome biome) {

            }

            @Override
            public boolean isBlockPowered() {
                return false;
            }

            @Override
            public boolean isBlockIndirectlyPowered() {
                return false;
            }

            @Override
            public boolean isBlockFacePowered(@NotNull BlockFace blockFace) {
                return false;
            }

            @Override
            public boolean isBlockFaceIndirectlyPowered(@NotNull BlockFace blockFace) {
                return false;
            }

            @Override
            public int getBlockPower(@NotNull BlockFace blockFace) {
                return 0;
            }

            @Override
            public int getBlockPower() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean isLiquid() {
                return false;
            }

            @Override
            public boolean isBuildable() {
                return false;
            }

            @Override
            public boolean isBurnable() {
                return false;
            }

            @Override
            public boolean isReplaceable() {
                return false;
            }

            @Override
            public boolean isSolid() {
                return false;
            }

            @Override
            public boolean isCollidable() {
                return false;
            }

            @Override
            public double getTemperature() {
                return 0;
            }

            @Override
            public double getHumidity() {
                return 0;
            }

            @Override
            public @NotNull PistonMoveReaction getPistonMoveReaction() {
                return null;
            }

            @Override
            public boolean breakNaturally() {
                return false;
            }

            @Override
            public boolean breakNaturally(@Nullable ItemStack itemStack) {
                return false;
            }

            @Override
            public boolean breakNaturally(boolean b, boolean b1) {
                return false;
            }

            @Override
            public boolean breakNaturally(@NotNull ItemStack itemStack, boolean b, boolean b1) {
                return false;
            }

            @Override
            public boolean breakNaturally(@NotNull ItemStack itemStack, boolean b, boolean b1, boolean b2) {
                return false;
            }

            @Override
            public void tick() {

            }

            @Override
            public void fluidTick() {

            }

            @Override
            public void randomTick() {

            }

            @Override
            public boolean applyBoneMeal(@NotNull BlockFace blockFace) {
                return false;
            }

            @Override
            public @NotNull Collection<ItemStack> getDrops() {
                return List.of();
            }

            @Override
            public @NotNull Collection<ItemStack> getDrops(@Nullable ItemStack itemStack) {
                return List.of();
            }

            @Override
            public @NotNull Collection<ItemStack> getDrops(@Nullable ItemStack itemStack, @Nullable Entity entity) {
                return List.of();
            }

            @Override
            public boolean isPreferredTool(@NotNull ItemStack itemStack) {
                return false;
            }

            @Override
            public float getBreakSpeed(@NotNull Player player) {
                return 0;
            }

            @Override
            public boolean isPassable() {
                return false;
            }

            @Override
            public @Nullable RayTraceResult rayTrace(@NotNull Location location, @NotNull Vector vector, double v, @NotNull FluidCollisionMode fluidCollisionMode) {
                return null;
            }

            @Override
            public @NotNull BoundingBox getBoundingBox() {
                return null;
            }

            @Override
            public @NotNull VoxelShape getCollisionShape() {
                return null;
            }

            @Override
            public boolean canPlace(@NotNull BlockData blockData) {
                return false;
            }

            @Override
            public @NotNull BlockSoundGroup getSoundGroup() {
                return null;
            }

            @Override
            public @NotNull SoundGroup getBlockSoundGroup() {
                return null;
            }

            @Override
            public @NotNull String getTranslationKey() {
                return "";
            }

            @Override
            public boolean isSuffocating() {
                return false;
            }

            @Override
            public @NotNull String translationKey() {
                return "";
            }

            @Override
            public void setMetadata(@NotNull String s, @NotNull MetadataValue metadataValue) {

            }

            @Override
            public @NotNull List<MetadataValue> getMetadata(@NotNull String s) {
                return List.of();
            }

            @Override
            public boolean hasMetadata(@NotNull String s) {
                return false;
            }

            @Override
            public void removeMetadata(@NotNull String s, @NotNull Plugin plugin) {

            }
        };
    }
}
