package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.block.*;
import com.gardensmc.gardensfurniture.custom.item.CustomCaveVineItem;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.itementity.ItemEntity;
import com.gardensmc.gardensfurniture.loader.ChunkFurnitureLoader;
import com.gardensmc.gardensfurniture.store.FurnitureCache;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import com.gardensmc.gardensfurniture.structure.StructureHandler;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import com.gardensmc.gardensfurniture.util.ProbabilityUtil;
import com.gardensmc.gardensfurniture.util.ProtectionUtil;
import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import de.tr7zw.nbtapi.NBT;
import io.papermc.paper.configuration.GlobalConfiguration;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.CaveVines;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gardensmc.gardensfurniture.custom.CustomBlockManager.*;
import static com.gardensmc.gardensfurniture.util.CustomDropUtil.dropCustomItem;

public class BlockListener implements Listener {

    private static final Set<Material> DISPLAY_BLOCK_TYPES = Set.of(Material.AIR, Material.BARRIER);

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent e) {
        if (!ProtectionUtil.canBuild(e.getBlock().getLocation(), e.getPlayer())) {
            return;
        }

        if (e.getBlock().getType() == Material.TRIPWIRE) {
            CustomItem customItem = CustomBlockManager.getItemFromLeaves(e.getBlock());
            if (customItem instanceof CustomPlaceableItem placeableItem && placeableItem.getCustomBlock() instanceof CustomTripwireBlock customTripwireBlock) {
                dropCustomItem(e, customItem, customTripwireBlock);
            }
        } else if (CustomBlockManager.isStoreBacked(e.getBlock().getType())) {
            CustomItem customItem = CustomBlockManager.getItemFromLeaves(e.getBlock());
            if (customItem instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomSaplingBlock) {
                // remove sapling display
                ItemEntity itemEntity = getItemEntity(e.getBlock().getLocation());
                if (itemEntity != null) {
                    itemEntity.getEntity().remove();
                }
            }
            dropCustomItem(e, customItem, true, e.getBlock().getType().toString().toLowerCase().endsWith("leaves"));
            // Doors are 2 blocks: vanilla removes both halves, but the other half keeps its furniture store
            if (customItem instanceof CustomPlaceableItem placeableItem
                    && placeableItem.getCustomBlock() instanceof CustomWaxedBlock waxedBlock
                    && waxedBlock.getWaxedType().toString().startsWith("DOOR")
                    && e.getBlock().getBlockData() instanceof org.bukkit.block.data.type.Door door) {
                var broken = e.getBlock();
                var partner = door.getHalf() == org.bukkit.block.data.Bisected.Half.BOTTOM
                        ? broken.getRelative(BlockFace.UP) : broken.getRelative(BlockFace.DOWN);
                FurnitureStoreHandler.INSTANCE.removeFurnitureStore(partner);
            }
        }

    }

    // When a waxed slab is combined into a double, the Bedrock client has already predicted the
    // slab in the block it clicked toward (the neighbour cell) and drew a ghost there; the server
    // merged it into a double here instead. Resend the neighbouring cells to the player so the
    // Bedrock client discards its prediction (Java clients just get a harmless redundant update).
//    @EventHandler(ignoreCancelled = true)
//    public void onWaxedSlabDoublePlace(BlockPlaceEvent e) {
//        var block = e.getBlockPlaced();
//        if (!CustomWaxedBlock.WaxedType.materials().contains(block.getType())
//                || !(block.getBlockData() instanceof org.bukkit.block.data.type.Slab slab)
//                || slab.getType() != org.bukkit.block.data.type.Slab.Type.DOUBLE) {
//            return;
//        }
//        var player = e.getPlayer();
//        System.out.println("Player did a double slab");
//        Bukkit.getScheduler().runTaskLater(GardensFurniture.plugin, () -> {
//            for (BlockFace face : new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.SELF}) {
//                var b = block.getRelative(face);
//                player.sendBlockChange(b.getLocation(), b.getBlockData());
//                System.out.println("Updating anove");
//            }
//        }, 2L);
//    }

    @EventHandler
    public void onSaplingOrTripwireBreak(BlockPhysicsEvent e) {
        var type = e.getBlock().getType();
        if (type == Material.OAK_SAPLING || type == Material.DARK_OAK_SAPLING || type == Material.TRIPWIRE || type == Material.FLOWER_POT) {
            Bukkit.getScheduler().runTaskLater(GardensFurniture.plugin, () -> {
                if (e.getBlock().getType() != type) {
                    if (type != Material.TRIPWIRE) {
                        ItemEntity itemEntity = getItemEntity(e.getBlock().getLocation());
                        if (itemEntity != null) {
                            if (type == Material.FLOWER_POT) {
                                // drop potted item
                                var plantedHeldItem = NBT.getPersistentData(itemEntity.getEntity(), nbt -> nbt.getItemStack("GardensFurniturePotted"));
                                if (plantedHeldItem != null) {
                                    e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), plantedHeldItem);
                                }
                            }
                            itemEntity.getEntity().remove();
                        }
                    }
                    if (type != Material.FLOWER_POT) {
                        FurnitureStoreHandler.INSTANCE.removeFurnitureStore(e.getBlock());
                    }
                }
            }, getCheckDelay(e.getSourceBlock().getType()));
        }
    }

    private int getCheckDelay(Material material) {
        return switch (material) {
            case WATER -> 10;
            case LAVA -> 35;
            default -> 1;
        };
    }

    // Cells whose custom sapling/tripwire was just destroyed without a player (physics, water, pistons,
    // explosions) -> tick the mark expires. Vanilla drops the plain sapling/string there; only drops
    // spawning in a marked cell become the custom item, so items dropped into a live cell aren't converted.
    private static final Map<Location, Integer> pendingCustomDrops = new HashMap<>();
    private static final Set<Material> CUSTOM_DROP_BLOCK_TYPES = Set.of(Material.OAK_SAPLING, Material.DARK_OAK_SAPLING, Material.TRIPWIRE);

    private static void markCustomDrop(Block block) {
        if (!CUSTOM_DROP_BLOCK_TYPES.contains(block.getType())) return;
        int now = Bukkit.getCurrentTick();
        pendingCustomDrops.values().removeIf(expiry -> expiry < now);
        pendingCustomDrops.put(block.getLocation(), now + 2);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void markDestroyedDrop(BlockDestroyEvent e) {
        markCustomDrop(e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void markWashedAwayDrop(BlockFromToEvent e) {
        markCustomDrop(e.getToBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void markExplodedDrops(BlockExplodeEvent e) {
        e.blockList().forEach(BlockListener::markCustomDrop);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void markExplodedDrops(EntityExplodeEvent e) {
        e.blockList().forEach(BlockListener::markCustomDrop);
    }

    @EventHandler
    public void onSaplingOrStringDrop(ItemSpawnEvent e) {
        var itemStack = e.getEntity().getItemStack();
        if (itemStack.getType() == Material.OAK_SAPLING || itemStack.getType() == Material.DARK_OAK_SAPLING || itemStack.getType() == Material.STRING) {
            var cell = e.getLocation().getBlock();
            var expiry = pendingCustomDrops.get(cell.getLocation());
            if (expiry == null || expiry < Bukkit.getCurrentTick()) return;
            CustomItem customItem = CustomBlockManager.getItemFromLeaves(cell);
            if (customItem instanceof CustomPlaceableItem placeable && (placeable.getCustomBlock() instanceof CustomSaplingBlock || placeable.getCustomBlock() instanceof CustomTripwireBlock)) {
                pendingCustomDrops.remove(cell.getLocation()); // one destroyed block, one converted drop
                e.getEntity().setItemStack(GardensFurniture.itemRegistry.buildItem(placeable));
            }
        }
    }

    @EventHandler
    public void onPick(PlayerPickItemEvent e) {
//        if (e.getSourceSlot() != -1) {
//            var source = e.getPlayer().getInventory().getItem(e.getSourceSlot());
//            if (source != null && source.getType() != Material.NOTE_BLOCK) {
//                return; // item is present in inventory and not a note block, we dont care
//            }
//        }
        final RayTraceResult rayTraceResult = e.getPlayer().rayTraceBlocks(6.0);
        if (rayTraceResult == null) return;
        final Block block = rayTraceResult.getHitBlock();
        if (block == null) return;
        var customBlock = CustomBlockManager.getCustomItemFromBlock(block);
        if (customBlock == null) return;
        e.setCancelled(true);

        for (int i = 0; i <= 35; i++) {
            var vanillaItem = e.getPlayer().getInventory().getItem(i);
            if (vanillaItem == null) continue;
            var invItem = GardensFurniture.itemRegistry.getCustomItem(vanillaItem);
            if (invItem != null && invItem.getIdentifier().equals(customBlock.getIdentifier())) {
                if (i < 9) { // already in hotbar
                    e.getPlayer().getInventory().setHeldItemSlot(i);
                } else {
                    swapItemSlots(e.getPlayer().getInventory(), i, e.getTargetSlot());
                }
                return;
            }
        }
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
            e.getPlayer().getInventory().setItem(e.getTargetSlot(), GardensFurniture.itemRegistry.buildItem(customBlock));
            e.getPlayer().getInventory().setHeldItemSlot(e.getTargetSlot());
        }
    }

    private void swapItemSlots(Inventory inv, int slotA, int slotB) {
        var itemA = inv.getItem(slotA);
        var itemB = inv.getItem(slotB);
        inv.setItem(slotA, itemB);
        inv.setItem(slotB, itemA);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (cancelNoteBlockExtend(e.getBlocks())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (cancelNoteBlockExtend(e.getBlocks())) {
            e.setCancelled(true);
        }
    }

    private boolean cancelNoteBlockExtend(List<Block> blocks) {
        for (Block block : blocks) {
            if (block.getType() == Material.NOTE_BLOCK && block.getBlockData() instanceof NoteBlock) {
                CustomItem customItem = CustomBlockManager.getItemFromLeaves(block);
                if (customItem != null) {
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onNotePlayed(final NotePlayEvent e) {
        if (CustomBlockManager.getItemFromLeaves(e.getBlock()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onConnectablePhysics(BlockPhysicsEvent e) {
        // physics events are extremely frequent; skip the entity lookup when nothing is connectable
        if (!GardensFurniture.itemRegistry.hasConnectableItems()) {
            return;
        }
        var block = e.getBlock();
        if (!DISPLAY_BLOCK_TYPES.contains(block.getType())) {
            return;
        }
        var display = getDisplayEntity(block.getLocation());
        if (display == null) {
            return;
        }
        var customItem = GardensFurniture.itemRegistry.getCustomItem(display.getItemStack());
        if (!(customItem instanceof CustomPlaceableItem placeable)
                || !(placeable.getCustomBlock() instanceof CustomDisplayBlock displayBlock)
                || !displayBlock.isConnectable()
        ) {
            return;
        }
        new ConnectableHandler(display, customItem).updateConnectableState();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onVineGrowth(BlockSpreadEvent e) {
        if (e.isCancelled()) return;
        Block root = e.getSource();
        if (root.getType() != Material.CAVE_VINES && root.getType() != Material.CAVE_VINES_PLANT) return;

        CustomItem customItem = CustomBlockManager.getItemFromLeaves(root);

        if (customItem instanceof CustomCaveVineItem customCaveVineItem && customCaveVineItem.getCustomBlock() instanceof CustomCaveVineBlock caveVineBlock) {
            if (caveVineBlock.isGrow()) {
                Block nextBlock = e.getNewState().getBlock();
                // state change of current vine
                if (nextBlock.getLocation().equals(root.getLocation())) {
                    // prevent berry growth - causes glowing
                    if (e.getNewState().getBlockData() instanceof CaveVines caveVines) {
                        if (caveVines.hasBerries() && !customCaveVineItem.getPlant().isGrowBerries() ) {
                            e.setCancelled(true);
                        }
                    }
                } else if (ProbabilityUtil.roll(caveVineBlock.getGrowthRate())) {
                    FurnitureStoreHandler.INSTANCE.addFurnitureStore(nextBlock, new FurnitureStore(customCaveVineItem.getIdentifier()), true);
                } else {
                    e.setCancelled(true);
                }
            } else {
                e.setCancelled(true);
            }
        }

    }

    @EventHandler(ignoreCancelled = true)
    public void onSaplingPlace(BlockPlaceEvent e) {
        var blockType = e.getBlockPlaced().getType();
        if (blockType == Material.OAK_SAPLING || blockType == Material.DARK_OAK_SAPLING) {
            var customItem = GardensFurniture.itemRegistry.getCustomItem(e.getItemInHand());
            if (customItem instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomSaplingBlock saplingBlock) {
                saplingBlock.onSaplingPlace(e.getPlayer(), e.getItemInHand(), placeable, e.getBlock().getLocation());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCaveVineFertilize(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (e.getHand() == null) return;
        if (e.getPlayer().getInventory().getItem(e.getHand()).getType() == Material.BONE_MEAL) {
            var type = e.getClickedBlock().getType();
            if (type == Material.CAVE_VINES || type == Material.CAVE_VINES_PLANT) {
                var customItem = CustomBlockManager.getItemFromLeaves(e.getClickedBlock());
                if (customItem != null) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSaplingGrow(StructureGrowEvent e) {
        var block = e.getLocation().getBlock();
        var customItem = CustomBlockManager.getItemFromLeaves(block);
        if (customItem != null && customItem.getMechanics() != null) {
            var saplingStructure = customItem.getMechanics().getSapling_structure();
            e.setCancelled(true);
            // datapack features can't be limited per block, so don't grow one next to someone else's claim
            if (!StructureHandler.INSTANCE.hasStructure(saplingStructure)
                    && ProtectionUtil.hasForeignClaimNear(e.getLocation(), StructureHandler.FEATURE_CLAIM_RADIUS)) {
                return;
            }
            // remove block(s)
            var blocks = e.getBlocks()
                    .stream()
                    .map(BlockState::getBlock).filter(b -> b.getY() == block.getY()) // can trigger dirt changes
                    .collect(Collectors.toSet());

            var location = e.getLocation();
            if (blocks.size() > 1) {
                // want to use block that is in correct corner -> lowest x and lowest z
                location = blocks.stream()
                        .sorted((o1, o2) -> {
                            if (o1.getX() == o2.getX() && o1.getZ() == o2.getZ()) {
                                return 0;
                            }
                            if (o1.getX() < o2.getX() && o1.getZ() < o2.getZ()) {
                                return -1;
                            }
                            if (o1.getX() < o2.getX() && o1.getZ() == o2.getZ()) {
                                return -1;
                            }
                            if (o1.getZ() < o2.getZ() && o1.getX() == o2.getX()) {
                                return -1;
                            }
                            return 1;
                        })
                        .toList()
                        .getFirst().getLocation();
            }

            blocks.forEach(b -> {
                b.setType(Material.AIR);
                FurnitureStoreHandler.INSTANCE.removeFurnitureStore(b);
                var itemEntity = CustomBlockManager.getItemEntity(b.getLocation());
                if (itemEntity != null) {
                    itemEntity.getEntity().remove();
                }
            });

            if (!StructureHandler.INSTANCE.spawnGrownStructure(saplingStructure, location)) {
                try {
                    handleFeature(saplingStructure, location);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static void handleFeature(String feature, Location location) throws ExecutionException, InterruptedException {
        var intersectingChunks = location.getWorld().getIntersectingChunks(BoundingBox.of(location, 20, 1, 20)).stream().toList();
        GlobalConfiguration.get().blockUpdates.disableNoteblockUpdates = true;
        location.getWorld().setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
        var chunkSnapshots = toChunkSnapshot(intersectingChunks);
        List<ChunkSnapshot> finalChunkSnapshots = chunkSnapshots;
        var existingNoteBlocks = CompletableFuture.supplyAsync(() -> finalChunkSnapshots.stream()
                .flatMap(chunk -> getExistingNoteBlocks(chunk, location.getWorld(), location.getBlockY()).stream())
                .collect(Collectors.toSet()))
                .get();
        // Run in the sapling's world explicitly: the console command source is anchored to the
        // primary/respawn overworld, so an unqualified "place feature" checks the position in the
        // wrong world and fails with "That position is not loaded". World#getKey() yields the
        // dimension key that "execute in" expects (minecraft:overworld / minecraft:<worldname>).
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute in %s run place feature gardens:%s %s %s %s".formatted(
                location.getWorld().getKey(), feature, location.getBlockX(), location.getBlockY(), location.getBlockZ()
        ));
        // run in future to allow feature to finish placing
        Bukkit.getScheduler().runTaskLater(GardensFurniture.plugin, () -> {
            var chunkSnapshots1 = toChunkSnapshot(intersectingChunks);
            Set<ChunkFurnitureLoader.FurnitureReplacement> replacements = null;
            try {
                replacements = CompletableFuture.supplyAsync(() -> chunkSnapshots1.stream()
                                .flatMap(chunk -> getReplacements(existingNoteBlocks, chunk, location.getWorld(), location.getBlockY()).stream())
                                .collect(Collectors.toSet()))
                        .get();
                replacements.forEach(replacement -> ChunkFurnitureLoader.INSTANCE.placeFurnitureBlock(replacement));
                intersectingChunks.forEach(chunk -> FurnitureCache.INSTANCE.refreshChunk(new FurnitureCache.ChunkKey(chunk)));
                GlobalConfiguration.get().blockUpdates.disableNoteblockUpdates = false;
                location.getWorld().setGameRule(GameRule.SEND_COMMAND_FEEDBACK, true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }, 1);

    }

    private static List<ChunkSnapshot> toChunkSnapshot(List<Chunk> chunks) {
        return chunks.stream()
                .map(Chunk::getChunkSnapshot)
                .toList();
    }

    private static List<ChunkFurnitureLoader.FurnitureReplacement> getReplacements(Set<FurnitureCache.BlockKey> existingNoteBlocks, ChunkSnapshot chunkSnapshot, World world, int yMin) {
        int yMax = yMin + 60;
        if (yMax > world.getMaxHeight()) {
            yMax = world.getMaxHeight();
        }
        var replacements = new ArrayList<ChunkFurnitureLoader.FurnitureReplacement>();
        for (int x = 0; x < 16; x++) {
            for (int y = yMax; y >= yMin; y--) {
                for (int z = 0; z < 16; z++) {
                    BlockData blockData = chunkSnapshot.getBlockData(x, y, z);
                    if (blockData instanceof NoteBlock noteBlock) {
                        if (existingNoteBlocks.contains(new FurnitureCache.BlockKey(chunkSnapshot.getX() * 16 + x, y, chunkSnapshot.getZ() * 16 + z))) {
                            continue;
                        }
                        String key = String.format(
                                "[%s, %s, %s]",
                                noteBlock.getNote().getId(),
                                noteBlock.getInstrument().toString().toLowerCase(),
                                noteBlock.isPowered()

                        );
                        int finalX = x;
                        int finalY = y;
                        int finalZ = z;
                        String mapping = ConfigUtil.generationMappings.getString(key);
                        if (mapping != null) {
                            replacements.add(new ChunkFurnitureLoader.FurnitureReplacement(chunkSnapshot.getWorldName(), chunkSnapshot.getX(), chunkSnapshot.getZ(), finalX, finalY, finalZ, mapping));
                        } else {
                            GardensFurniture.plugin.getLogger().severe("Could not find mapping with key: " + key);
                        }
                    }
                }
            }
        }
        return replacements;
    }

    private static Set<FurnitureCache.BlockKey> getExistingNoteBlocks(ChunkSnapshot chunkSnapshot, World world, int yMin) {
        int yMax = yMin + 60;
        if (yMax > world.getMaxHeight()) {
            yMax = world.getMaxHeight();
        }
        var noteBlocks = new HashSet<FurnitureCache.BlockKey>();
        for (int x = 0; x < 16; x++) {
            for (int y = yMax; y >= yMin; y--) {
                for (int z = 0; z < 16; z++) {
                    BlockData blockData = chunkSnapshot.getBlockData(x, y, z);
                    if (blockData instanceof NoteBlock) {
                        noteBlocks.add(new FurnitureCache.BlockKey(chunkSnapshot.getX() * 16 + x, y, chunkSnapshot.getZ() * 16 + z));
                    }
                }
            }
        }
        return noteBlocks;
    }
}
