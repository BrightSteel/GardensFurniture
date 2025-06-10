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
import org.bukkit.block.data.type.CaveVines;
import org.bukkit.block.data.type.Comparator;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.RayTraceResult;

import java.util.*;
import java.util.stream.Stream;

import static com.gardensmc.gardensfurniture.custom.CustomBlockManager.getDisplayEntity;
import static com.gardensmc.gardensfurniture.custom.CustomBlockManager.packetTypes;
import static com.gardensmc.gardensfurniture.util.CustomDropUtil.dropCustomItem;

public class BlockListener implements Listener {

    private static final Set<Material> DISPLAY_BLOCK_TYPES = Set.of(Material.AIR, Material.BARRIER);

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.isCancelled()) return;
        if (!ProtectionUtil.canBuild(e.getBlock().getLocation(), e.getPlayer())) {
            return;
        }

        if (e.getBlock().getType() == Material.TRIPWIRE) {
            CustomItem customItem = CustomBlockManager.getItemFromLeaves(e.getBlock());
            if (customItem instanceof CustomPlaceableItem placeableItem && placeableItem.getCustomBlock() instanceof CustomTripwireBlock customTripwireBlock) {
                dropCustomItem(e, customItem, customTripwireBlock);
            }
        } else if (Stream.concat(Arrays.stream(packetTypes), CustomWaxedBlock.WaxedType.materials().stream()).toList().contains(e.getBlock().getType())) {
            CustomItem customItem = CustomBlockManager.getItemFromLeaves(e.getBlock());
            dropCustomItem(e, customItem, true, e.getBlock().getType().toString().toLowerCase().endsWith("leaves"));
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
}
