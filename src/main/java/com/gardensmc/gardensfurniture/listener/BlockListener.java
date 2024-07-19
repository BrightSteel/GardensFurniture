package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.block.CustomBuddingBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomCrystalBlock;
import com.gardensmc.gardensfurniture.custom.item.CustomBlockItem;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.block.CustomTripwireBlock;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import com.gardensmc.gardensfurniture.util.ProtectionUtil;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.AmethystCluster;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

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
        else if (Arrays.asList(packetTypes).contains(e.getBlock().getType())) {
            CustomItem customItem = CustomBlockManager.getItemFromLeaves(e.getBlock());
            dropCustomItem(e, customItem, true);
        }

    }

    @EventHandler
    public void updateBlockState(BlockPhysicsEvent e) {

        if (e.isCancelled()) return;
        Block[] possibles = new Block[]{
                e.getBlock(), e.getSourceBlock(), e.getSourceBlock().getRelative(0, 1, 0), e.getSourceBlock().getRelative(0, -1, 0)
        };

        for (Block block : possibles) {
            String[] redstone_types = new String[]{
                    "lever", "redstone_torch", "redstone_wall_torch", "redstone_block", "redstone_wire"
            };
            if (Arrays.asList(redstone_types).contains(e.getChangedType().name().toLowerCase()) && block.getType() == Material.NOTE_BLOCK) {
                CustomItem customItem = CustomBlockManager.getItemFromNoteBlock((NoteBlock) block.getBlockData());
                if (customItem != null) {
                    e.setCancelled(true);
                }
            }
            else if (block.getType() == Material.NOTE_BLOCK) {
                e.setCancelled(true);
            }
        }

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
    public void playNoteBlockNote(NotePlayEvent e) {
        if (e.isCancelled()) return;
        if (e.getInstrument() != Instrument.PIANO) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockGrowth(BlockGrowEvent e) {
        var block = e.getBlock();
        if (isAmethystBud(block.getType())) {
            AmethystCluster clusterData = (AmethystCluster) block.getBlockData(); // safe to cast?
            var crystal = CustomBlockManager.getCustomCrystalFromPos(block.getWorld(), block.getX(), block.getY(), block.getZ());
            if (crystal != null) {
                var newMaterial = e.getNewState().getType();
                // remove old crystal
                CustomBlockManager.breakBlock(null, block.getLocation());
                // place new crystal
                var newIdentifier = GardensFurniture.itemRegistry.crystalRegistry.getCrystalIdentifier(crystal.getCrystalId(), getCrystalType(newMaterial));
                CustomPlaceableItem newCustomItem = (CustomPlaceableItem) GardensFurniture.itemRegistry.getItem(newIdentifier);
                CustomBlockManager.attemptPlacement(null, null, newCustomItem, block.getLocation(), clusterData.getFacing(), false);
            }
        }
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent e) {
        var block = e.getSource();
        if (block.getType() != Material.BUDDING_AMETHYST) {
            return;
        }
        AmethystCluster clusterData = (AmethystCluster) e.getNewState().getBlockData(); // safe to cast ?
        var customItem = CustomBlockManager.getItemFromLeaves(block);
        if (customItem instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomBuddingBlock buddingBlock) {
            var crystalId = buddingBlock.getCrystalId();
            var budIdentifier = GardensFurniture.itemRegistry.crystalRegistry.getCrystalIdentifier(crystalId, CustomCrystalBlock.CrystalType.SMALL_BUD);
            if (budIdentifier == null) {
                GardensFurniture.plugin.getLogger().log(Level.SEVERE, "Failed to get small bud for crystalId: " + crystalId);
                return;
            }
            var customBudItem = GardensFurniture.itemRegistry.getItem(budIdentifier);
            CustomBlockManager.attemptPlacement(null, null, (CustomPlaceableItem) customBudItem, e.getNewState().getLocation(), clusterData.getFacing(), false);
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
}
