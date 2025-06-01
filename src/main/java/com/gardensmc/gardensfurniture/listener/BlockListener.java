package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.block.*;
import com.gardensmc.gardensfurniture.custom.item.CustomCaveVineItem;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import com.gardensmc.gardensfurniture.util.ProbabilityUtil;
import com.gardensmc.gardensfurniture.util.ProtectionUtil;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.CaveVines;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.RayTraceResult;

import java.util.*;
import java.util.stream.Stream;

import static com.gardensmc.gardensfurniture.custom.CustomBlockManager.getDisplayEntity;
import static com.gardensmc.gardensfurniture.util.CustomDropUtil.dropCustomItem;

public class BlockListener implements Listener {

    private static final Set<Material> DISPLAY_BLOCK_TYPES = Set.of(Material.AIR, Material.BARRIER);

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.isCancelled()) return;
        if (!ProtectionUtil.canBuild(e.getBlock().getLocation(), e.getPlayer())) {
            return;
        }

        Material[] packetTypes = new Material[] {
                Material.SPRUCE_LEAVES,
                Material.CHERRY_LEAVES,
                Material.CAVE_VINES,
                Material.CAVE_VINES_PLANT
        };

        if (e.getBlock().getType() == Material.NOTE_BLOCK && e.getBlock().getBlockData() instanceof NoteBlock noteBlock) {
            CustomItem customItem = CustomBlockManager.getItemFromNoteBlock(noteBlock);
            dropCustomItem(e, customItem, false);
        }
        else if (e.getBlock().getType() == Material.TRIPWIRE) {
            CustomItem customItem = CustomBlockManager.getItemFromLeaves(e.getBlock());
            if (customItem instanceof CustomPlaceableItem placeableItem && placeableItem.getCustomBlock() instanceof CustomTripwireBlock customTripwireBlock) {
                dropCustomItem(e, customItem, customTripwireBlock);
            }
        }
        else if (Stream.concat(Arrays.stream(packetTypes), CustomWaxedBlock.WaxedType.materials().stream()).toList().contains(e.getBlock().getType())) {
            CustomItem customItem = CustomBlockManager.getItemFromLeaves(e.getBlock());
            dropCustomItem(e, customItem, true);
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(final BlockPhysicsEvent event) {
        final Block block = event.getBlock();
        final Block aboveBlock = block.getRelative(BlockFace.UP);
        final Block belowBlock = block.getRelative(BlockFace.DOWN);
        // If block below is NoteBlock, it will be affected by the break
        // Call updateAndCheck from it to fix vertical stack of NoteBlocks
        // if belowBlock is not a NoteBlock we must ensure the above is not, if it is call updateAndCheck from block
        if (belowBlock.getType() == Material.NOTE_BLOCK) {
            event.setCancelled(true);
            updateAndCheck(belowBlock);
        } else if (aboveBlock.getType() == Material.NOTE_BLOCK) {
            event.setCancelled(true);
            updateAndCheck(aboveBlock);
        }
        if (block.getType() == Material.NOTE_BLOCK) {
            event.setCancelled(true);
            updateAndCheck(block);
        }
    }

    public void updateAndCheck(Block block) {
        final Block blockAbove = block.getRelative(BlockFace.UP);
        if (blockAbove.getType() == Material.NOTE_BLOCK)
            blockAbove.getState().update(true, true);
        Block nextBlock = blockAbove.getRelative(BlockFace.UP);
        if (nextBlock.getType() == Material.NOTE_BLOCK) updateAndCheck(blockAbove);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNoteblockPowered(final GenericGameEvent event) {
        Block block = event.getLocation().getBlock();

        Location eLoc = block.getLocation();
        if (!isLoaded(event.getLocation()) || !isLoaded(eLoc)) return;


        if (event.getEvent() != GameEvent.NOTE_BLOCK_PLAY) return;
        if (block.getType() != Material.NOTE_BLOCK) return;
        NoteBlock data = (NoteBlock) block.getBlockData().clone();
        if (CustomBlockManager.getItemFromNoteBlock(data) != null) {
            Bukkit.getScheduler().runTaskLater(GardensFurniture.plugin, () -> block.setBlockData(data, false), 1L);
        }
    }

    public static boolean isLoaded(World world, Location loc) {
        return world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    public static boolean isLoaded(Location loc) {
        return loc.getWorld() != null && isLoaded(loc.getWorld(), loc);
    }

    @EventHandler
    public void placeNoteBlock(BlockPlaceEvent e) {
        if (e.isCancelled()) return;

        if (e.getBlockPlaced().getType() == Material.NOTE_BLOCK) {
            e.getBlockPlaced().setType(Material.NOTE_BLOCK, false);
//            CustomItem customItem = CustomBlockManager.getItemFromNoteBlock((NoteBlock) e.getBlockPlaced().getBlockData());
//            if (customItem == null) {
//
//            }
        }
    }

    // If block is not a custom block, play the correct sound according to the below block or default
    @EventHandler(priority = EventPriority.NORMAL)
    public void onNotePlayed(final NotePlayEvent event) {
        if (event.getInstrument() != Instrument.PIANO) event.setCancelled(true);
        else {
            if (instrumentMap.isEmpty()) instrumentMap = getInstrumentMap();
            String blockType = event.getBlock().getRelative(BlockFace.DOWN).getType().toString().toLowerCase(Locale.ROOT);
            Instrument fakeInstrument = instrumentMap.entrySet().stream().filter(e -> e.getValue().contains(blockType)).map(Map.Entry::getKey).findFirst().orElse(Instrument.PIANO);
            // This is deprecated, but seems to be without reason
            event.setInstrument(fakeInstrument);
        }
    }

    @EventHandler
    public void onPick(PlayerPickItemEvent e) {
        if (e.getSourceSlot() != -1) {
            var source = e.getPlayer().getInventory().getItem(e.getSourceSlot());
            if (source != null && source.getType() != Material.NOTE_BLOCK) {
                return; // item is present in inventory and not a note block, we dont care
            }
        }
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
            if (block.getType() == Material.NOTE_BLOCK && block.getBlockData() instanceof NoteBlock noteBlock) {
                CustomItem customItem = CustomBlockManager.getItemFromNoteBlock(noteBlock);
                if (customItem != null) {
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onConnectablePhysics(BlockPhysicsEvent e) {
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

    // Used to determine what instrument to use when playing a note depending on below block
    public static Map<Instrument, List<String>> instrumentMap = new HashMap<>();

    private static Map<Instrument, List<String>> getInstrumentMap() {
        Map<Instrument, List<String>> map = new HashMap<>();
        map.put(Instrument.BELL, List.of("gold_block"));
        map.put(Instrument.BASS_DRUM, Arrays.asList("stone", "netherrack", "bedrock", "observer", "coral", "obsidian", "anchor", "quartz"));
        map.put(Instrument.FLUTE, List.of("clay"));
        map.put(Instrument.CHIME, List.of("packed_ice"));
        map.put(Instrument.GUITAR, List.of("wool"));
        map.put(Instrument.XYLOPHONE, List.of("bone_block"));
        map.put(Instrument.IRON_XYLOPHONE, List.of("iron_block"));
        map.put(Instrument.COW_BELL, List.of("soul_sand"));
        map.put(Instrument.DIDGERIDOO, List.of("pumpkin"));
        map.put(Instrument.BIT, List.of("emerald_block"));
        map.put(Instrument.BANJO, List.of("hay_bale"));
        map.put(Instrument.PLING, List.of("glowstone"));
        map.put(Instrument.BASS_GUITAR, List.of("wood"));
        map.put(Instrument.SNARE_DRUM, Arrays.asList("sand", "gravel", "concrete_powder", "soul_soil"));
        map.put(Instrument.STICKS, Arrays.asList("glass", "sea_lantern", "beacon"));

        return map;
    }
}
