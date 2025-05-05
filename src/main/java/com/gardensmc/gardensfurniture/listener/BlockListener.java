package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.block.*;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.item.PlaceOptions;
import com.gardensmc.gardensfurniture.util.ProtectionUtil;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.AmethystCluster;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Stream;

import static com.gardensmc.gardensfurniture.util.CustomDropUtil.dropCustomItem;

public class BlockListener implements Listener {

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



//    @EventHandler(ignoreCancelled = true)
//    public void updateBlockState(BlockPhysicsEvent e) {
//        Block[] possibles = new Block[]{
//                e.getBlock(), e.getSourceBlock(), e.getSourceBlock().getRelative(0, 1, 0), e.getSourceBlock().getRelative(0, -1, 0)
//        };
//
//        for (Block block : possibles) {
//            String[] redstone_types = new String[]{
//                    "lever", "redstone_torch", "redstone_wall_torch", "redstone_block", "redstone_wire"
//            };
//            if (Arrays.asList(redstone_types).contains(e.getChangedType().name().toLowerCase()) && block.getType() == Material.NOTE_BLOCK) {
//                CustomItem customItem = CustomBlockManager.getItemFromNoteBlock((NoteBlock) block.getBlockData());
//                if (customItem != null) {
//                    e.setCancelled(true);
//                }
//            }
//            else if (block.getType() == Material.NOTE_BLOCK) {
//                e.setCancelled(true);
//            }
//        }
//
//    }

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
    public void onClack(InventoryCreativeEvent e) {
        System.out.println(e.getClick());
    }

    @EventHandler
    public void onPick(PlayerPickItemEvent e) {
        System.out.println("Picking item");
        if (e.getSourceSlot() != -1) {
            var source = e.getPlayer().getInventory().getItem(e.getSourceSlot());
            if (source != null && source.getType() != Material.NOTE_BLOCK) {
                return; // item is present in inventory and not a note block, we dont care
            }
        }
        System.out.println(e.getSourceSlot());
        final RayTraceResult rayTraceResult = e.getPlayer().rayTraceBlocks(6.0);
        if (rayTraceResult == null) return;
        final Block block = rayTraceResult.getHitBlock();
        if (block == null) return;
        System.out.println(block.getType());
        var customBlock = CustomBlockManager.getCustomItemFromBlock(block);
        System.out.println(customBlock);
        if (customBlock == null) return;
        System.out.println(customBlock.getIdentifier());
        e.setCancelled(true);

        for (int i = 0; i <= 35; i++) {
            var vanillaItem = e.getPlayer().getInventory().getItem(i);
            if (vanillaItem == null) continue;
            var invItem = GardensFurniture.itemRegistry.getCustomItem(vanillaItem);
            if (invItem != null && invItem.getIdentifier().equals(customBlock.getIdentifier())) {
                if (i < 9) { // already in hotbar
                    e.getPlayer().getInventory().setHeldItemSlot(i);
                } else {
//                e.setSourceSlot(i); not working ? maybe becayse its paper not sure
                    swapItemSlots(e.getPlayer().getInventory(), i, e.getTargetSlot());
                    System.out.println("Doing source item");
                }
                return;
            }
        }
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
            System.out.println("Did other thing");
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMiddleClick(final InventoryClickEvent event) {
        System.out.println(event.getClick());
        if (event.getClick() != ClickType.MIDDLE) return;
        final Player player = (Player) event.getInventory().getHolder();
        if (player == null) return;
        if (event.getCursor().getType() == Material.NOTE_BLOCK) {
            final RayTraceResult rayTraceResult = player.rayTraceBlocks(6.0);
            if (rayTraceResult == null) return;
            final Block block = rayTraceResult.getHitBlock();
            if (block == null) return;
            System.out.println(block.getType());
            var customBlock = CustomBlockManager.getCustomItemFromBlock(block);
            System.out.println(customBlock);

            if (customBlock == null) return;
            System.out.println(customBlock.getIdentifier());



            for (int i = 0; i <= 8; i++) {
                if (player.getInventory().getItem(i) == null) continue;
                var hotbarItem = GardensFurniture.itemRegistry.getCustomItem(player.getInventory().getItem(i));
                if (hotbarItem != null && hotbarItem.getIdentifier().equals(customBlock.getIdentifier())) {
                    player.getInventory().setHeldItemSlot(i);
                    System.out.println("Doing hotbar item");
                    event.setCancelled(true);
                    return;
                }
            }
            event.setCursor(GardensFurniture.itemRegistry.buildItem(customBlock));
        }
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

//    @EventHandler
//    public void playNoteBlockNote(NotePlayEvent e) {
//        if (e.isCancelled()) return;
//        if (e.getInstrument() != Instrument.PIANO) {
//            e.setCancelled(true);
//        }
//    }

    @EventHandler
    public void onBlockGrowth(BlockGrowEvent e) {
        var block = e.getBlock();
        if (isAmethystBud(block.getType())) {
            AmethystCluster clusterData = (AmethystCluster) block.getBlockData(); // safe to cast?
            var crystal = CustomBlockManager.getDisplayEntity(block.getLocation());
            var newMaterial = e.getNewState().getType();
            if (crystal != null) {
                var model = crystal.getItemStack().getItemMeta().getItemModel().asString();
                var crystalId = GardensFurniture.itemRegistry.crystalRegistry.getCrystalIdentifier(CustomCrystalBlock.CrystalType.fromMaterial(block.getType()), model);
                var customItem = GardensFurniture.itemRegistry.getItem(crystalId);
                // remove old crystal
                crystal.remove();
                if (customItem instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomCrystalBlock crystalBlock) {
                    // place new crystal
                    var newIdentifier = GardensFurniture.itemRegistry.crystalRegistry.getCrystalIdentifier(crystalBlock.getCrystalId(), getCrystalType(newMaterial));
                    CustomPlaceableItem newCustomItem = (CustomPlaceableItem) GardensFurniture.itemRegistry.getItem(newIdentifier);
                    crystalBlock.placeDisplayEntity(newCustomItem, block.getLocation(), clusterData.getFacing());
                } else {
                    // normal amethyst - rotated to better fit display "fixed" setting
                    var amethystBudDupe = GardensFurniture.itemRegistry.getItem(newMaterial.toString().toLowerCase());
                    if (amethystBudDupe instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomDisplayBlock displayBlock) {
                        displayBlock.placeBlock(null, null, placeable, e.getBlock().getLocation(), new PlaceOptions().setBlockFace(clusterData.getFacing()));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent e) {
        var block = e.getSource();
        if (block.getType() != Material.BUDDING_AMETHYST) {
            return;
        }
        AmethystCluster clusterData = (AmethystCluster) e.getNewState().getBlockData();
        var newMaterial = e.getNewState().getType();
        var customItem = CustomBlockManager.getItemFromLeaves(block);
        if (customItem instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomBuddingBlock buddingBlock) {
            var crystalId = buddingBlock.getCrystalId();
            var budIdentifier = GardensFurniture.itemRegistry.crystalRegistry.getCrystalIdentifier(crystalId, CustomCrystalBlock.CrystalType.fromMaterial(newMaterial));
            if (budIdentifier == null) {
                GardensFurniture.plugin.getLogger().log(Level.SEVERE, "Failed to get small bud for crystalId: " + crystalId);
                return;
            }
            var customBudItem = (CustomPlaceableItem) GardensFurniture.itemRegistry.getItem(budIdentifier);
            if (customBudItem != null) {
                customBudItem.placeBlock(null, null, e.getNewState().getLocation(), new PlaceOptions().setBlockFace(clusterData.getFacing()));
            }
        } else {
            // normal amethyst - rotated to better fit display "fixed" setting
            var amethystBudDupe = GardensFurniture.itemRegistry.getItem(newMaterial.toString().toLowerCase());
            if (amethystBudDupe instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomDisplayBlock displayBlock) {
                displayBlock.placeBlock(null, null, placeable, e.getBlock().getLocation(), new PlaceOptions().setBlockFace(clusterData.getFacing()));
            }
        }
    }

    @EventHandler
    public void onCrystalOrCarpetBreak(BlockBreakEvent e) {
        if (e.isCancelled()) return;

        var block = e.getBlock();
        if (CustomCrystalBlock.isCrystalMaterial(block.getType()) || block.getType() == Material.BROWN_CARPET || block.getType() == Material.OAK_SAPLING) {
            var display = CustomBlockManager.getDisplayEntity(block.getLocation());
            if (display != null) {
                // remove crystal one tick later to allow blockDropItem event to still see the entity
                Bukkit.getScheduler().runTaskLater(GardensFurniture.plugin, display::remove, 1L);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCarpetDrop(BlockDropItemEvent e) {
        var block = e.getBlock(); // air
        if (e.getBlockState().getType() == Material.BROWN_CARPET) {
            var display = CustomBlockManager.getDisplayEntity(block.getLocation());
            if (display == null || display.getItemStack() == null) return; // in-case we WE it in or something
            var customItem = GardensFurniture.itemRegistry.getCustomItem(display.getItemStack());
            if (customItem instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomCarpetBlock) {
                for (Item item : e.getItems()) {
                    var count = item.getItemStack().getAmount();
                    if (item.getItemStack().getType() == Material.BROWN_CARPET) {
                        var itemStack = GardensFurniture.itemRegistry.buildItem(customItem);
                        if (itemStack != null) {
                            itemStack.setAmount(count);
                            item.setItemStack(itemStack);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onCrystalDrop(BlockDropItemEvent e) {
        if (e.isCancelled()) return;

        var block = e.getBlock(); // AIR at this point
        String customCrystalId = null;
        if (e.getBlockState().getType() == Material.BUDDING_AMETHYST) {
            var customItem = CustomBlockManager.getItemFromLeaves(block);
            if (customItem instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomBuddingBlock buddingBlock) {
                customCrystalId = buddingBlock.getCrystalId();
            }
        } else if (CustomCrystalBlock.isCrystalMaterial(e.getBlockState().getType())) {
            var display = CustomBlockManager.getDisplayEntity(block.getLocation());
            var model = display.getItemStack().getItemMeta().getItemModel().asString();
            var crystalId = GardensFurniture.itemRegistry.crystalRegistry.getCrystalIdentifier(CustomCrystalBlock.CrystalType.fromMaterial(display.getItemStack().getType()), model);
            var customItem = GardensFurniture.itemRegistry.getItem(crystalId);
            if (customItem instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomCrystalBlock crystalBlock) {
                customCrystalId = crystalBlock.getCrystalId();
            }
        }

        if (customCrystalId != null) {
            for (Item item : e.getItems()) {
               var itemStack = item.getItemStack();
               var count = itemStack.getAmount();
               ItemStack crystalItemStack = null;
               if (itemStack.getType() == Material.AMETHYST_SHARD) {
                   crystalItemStack = GardensFurniture.itemRegistry.buildItem(customCrystalId.toLowerCase() + "_shard");
               } else if (CustomCrystalBlock.isCrystalMaterial(itemStack.getType())) {
                   var crystalType = CustomCrystalBlock.CrystalType.fromMaterial(itemStack.getType());
                   var crystalIdentifier = GardensFurniture.itemRegistry.crystalRegistry.getCrystalIdentifier(customCrystalId, crystalType);
                   crystalItemStack = GardensFurniture.itemRegistry.buildItem(crystalIdentifier);
               }

               if (crystalItemStack != null) {
                   crystalItemStack.setAmount(count);
                   item.setItemStack(crystalItemStack);
               }
            }
        }
    }

    @EventHandler
    public void onDisplayEntityPhysics(BlockPhysicsEvent e) {
        if (e.isCancelled()) return;

        if (e.getSourceBlock().getType() == Material.AIR) {
            Bukkit.getScheduler().runTaskLater(GardensFurniture.plugin, () -> {
                // call once block has actually broken - lingering display entity for dupes need to get removed
                // make sure block is now "hovering" and should be broken
                if (e.getBlock().getType() == Material.AIR && e.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
                    var display = CustomBlockManager.getDisplayEntity(e.getBlock().getLocation());
                    if (display != null) {
                        CustomItem customItem = GardensFurniture.itemRegistry.getCustomItem(display.getItemStack());
                        if (customItem instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomDisplayBlock displayBlock && displayBlock.isGravity()) {
                            display.remove();
                            // todo call BlockManager.break, and also make sure that removes interaction entities
                        }
                    }
                }
            }, 1);
        }
    }

    public static boolean isAmethystBud(Material material) {
        return material == Material.SMALL_AMETHYST_BUD
                || material == Material.MEDIUM_AMETHYST_BUD
                || material == Material.LARGE_AMETHYST_BUD;
    }

    private CustomCrystalBlock.CrystalType getCrystalType(Material material) {
        return Arrays.stream(CustomCrystalBlock.CrystalType.values()).filter(crystalType -> crystalType.getCrystalMaterial() == material).findFirst().orElseThrow();
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
