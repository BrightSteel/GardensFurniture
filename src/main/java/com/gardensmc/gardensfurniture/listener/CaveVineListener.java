package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.block.*;
import com.gardensmc.gardensfurniture.custom.item.*;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.AmethystCluster;
import org.bukkit.block.data.type.CaveVines;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class CaveVineListener implements Listener {

    private static final String AMETHYST_BUDDING_ID = "AMETHYST";

    // cave vine and amethyst natural placing
    @EventHandler(priority = EventPriority.LOW)
    public void blockPlaceEvent(BlockPlaceEvent e) {
        if (e.isCancelled()) return;

        var blockPlaced = e.getBlockPlaced();
        ItemStack itemStack = e.getItemInHand();
        if (itemStack.getType() == Material.GLOW_BERRIES) {
            CustomItem customItem = GardensFurniture.itemRegistry.getCustomItem(itemStack);
            if (customItem instanceof CustomCaveVineItem customCaveVineItem) {
                FurnitureStoreHandler.INSTANCE.addFurnitureStore(blockPlaced, new FurnitureStore(customCaveVineItem.getIdentifier()), true);
            }
        } else if (CustomCrystalBlock.isCrystalMaterial(itemStack.getType())) {
            CustomItem customItem = GardensFurniture.itemRegistry.getCustomItem(itemStack);
            AmethystCluster crystal = (AmethystCluster) blockPlaced.getBlockData();
            // make sure budding block matches bud if placing against one
            boolean budding = e.getBlockAgainst().getType() == Material.BUDDING_AMETHYST;
            if (customItem instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomCrystalBlock crystalBlock) {
                if (budding && !crystalBlock.getCrystalId().equals(getBuddingCrystalId(e.getBlockAgainst()))) {
                    e.setCancelled(true); // in-compatible budding
                    return;
                }
                // vanilla already placed the bud, let's place the item frame
                crystalBlock.placeDisplayEntity(placeable, blockPlaced.getLocation(), crystal.getFacing());
                // furniture store
//                FurnitureStoreHandler.INSTANCE.addFurnitureStore(blockPlaced, new FurnitureStore(customItem.getIdentifier()));
            } else {
                if (budding && !getBuddingCrystalId(e.getBlockAgainst()).equals(AMETHYST_BUDDING_ID)) {
                    e.setCancelled(true);
                    return;
                }
                // normal amethyst - rotated to better fit display "fixed" setting
                var amethystBudDupe = GardensFurniture.itemRegistry.getItem(itemStack.getType().toString().toLowerCase());
                if (amethystBudDupe instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomDisplayBlock displayBlock) {
                    displayBlock.placeBlock(null, null, placeable, blockPlaced.getLocation(), new PlaceOptions().setBlockFace(crystal.getFacing()));
                }
            }

        } else if (itemStack.getType() == Material.BROWN_CARPET) {
            var carpetDupe = GardensFurniture.itemRegistry.getItem("brown_carpet");
            if (carpetDupe == null) {
                Bukkit.getLogger().log(Level.SEVERE, "Missing brown carpet dupe!");
            } else if (carpetDupe instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomDisplayBlock displayBlock) {
                displayBlock.placeBlock(null, null, placeable, blockPlaced.getLocation());
            }
        } else if (itemStack.getType() == Material.OAK_SAPLING) {
            CustomItem customItem = GardensFurniture.itemRegistry.getCustomItem(itemStack);
            if (customItem instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomSaplingBlock customSaplingBlock) {
                // vanilla already placed the sapling, let's place the item frame
                customSaplingBlock.placeDisplayEntity(placeable, blockPlaced.getLocation(), BlockFace.UP);
            } else {
                var saplingDupe = GardensFurniture.itemRegistry.getItem("oak_sapling");
                if (saplingDupe instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomDisplayBlock displayBlock) {
                    displayBlock.placeBlock(null, null, placeable, blockPlaced.getLocation());
                }
            }
        }
    }

    private String getBuddingCrystalId(Block block) {
        var furnitureStore = FurnitureStoreHandler.INSTANCE.getFurnitureStore(block);
        if (furnitureStore.getItemIdentifier() != null) {
            var customBudding = GardensFurniture.itemRegistry.getItem(furnitureStore.getItemIdentifier());
            if (customBudding instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomBuddingBlock buddingBlock) {
                return buddingBlock.getCrystalId();
            }
        }
        return AMETHYST_BUDDING_ID;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onVineGrowth(BlockSpreadEvent e) {
        if (e.isCancelled()) return;
        Block root = e.getSource();
        if (root.getType() != Material.CAVE_VINES && root.getType() != Material.CAVE_VINES_PLANT) return;

        CustomItem customItem = CustomBlockManager.getItemFromLeaves(root);

            if (customItem instanceof CustomCaveVineItem customCaveVineItem) {
                Block nextBlock = e.getNewState().getBlock();
                // state change of current vine
                if (nextBlock.getLocation().equals(root.getLocation())) {
                    if (e.getNewState().getBlockData() instanceof CaveVines caveVines) {
                        // prevent glowing server-side - actual issue it seems like...
                        if (!customCaveVineItem.getPlant().allowGlow() && caveVines.isBerries()) {
                            e.setCancelled(true);
                        }
                    }
                } else if (customItem.getIdentifier().equalsIgnoreCase("willow_vines") && ThreadLocalRandom.current().nextInt(10) != 0) { // lower growth rate to 1/10 chance
                    e.setCancelled(true);
                } else {
                    FurnitureStoreHandler.INSTANCE.addFurnitureStore(nextBlock, new FurnitureStore(customCaveVineItem.getIdentifier()), true);
                }
            }
            else if (customItem instanceof CustomCaveVineDirectional) {
                e.setCancelled(true);
            }

    }

    @EventHandler(priority = EventPriority.LOW)
    public void onRightClickLeaves(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (e.getHand() == null) return;

        if (e.getBlockFace() != BlockFace.DOWN) return;

        // only want to enable placement on leaves
        if (e.getClickedBlock() == null || !e.getClickedBlock().getType().toString().toUpperCase().endsWith("LEAVES")) {
            return;
        }

        Block target = e.getClickedBlock().getRelative(e.getBlockFace());
        if (target.getType() != Material.AIR) return;
        BlockState oldState = target.getState();

        ItemStack itemStack = player.getInventory().getItem(e.getHand());
        if (itemStack == null || itemStack.getType() != Material.GLOW_BERRIES) return;

        CustomItem customItem = GardensFurniture.itemRegistry.getCustomItem(itemStack);
        if (customItem instanceof CustomCaveVineItem customCaveVineItem) {

            // fire preliminary block place event to make sure itll go through
            BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, oldState, e.getClickedBlock(), itemStack, player, true, e.getHand());
            Bukkit.getPluginManager().callEvent(blockPlaceEvent);
            if (blockPlaceEvent.isCancelled()) return;

            // update chunk nbt
            FurnitureStoreHandler.INSTANCE.addFurnitureStore(target, new FurnitureStore(customCaveVineItem.getIdentifier()), true);

            // swing hand
            switch (e.getHand()) {
                case HAND -> e.getPlayer().swingMainHand();
                case OFF_HAND -> e.getPlayer().swingOffHand();
            }

            // place item
            CustomCaveVineBlock b = customCaveVineItem.getPlant();
            target.setType(Material.CAVE_VINES);
//            BlockData blockData = target.getBlockData();
//            if (blockData instanceof CaveVines caveVines) {
//                caveVines.setAge(b.getAge());
//            }

            // use up item
            if (e.getPlayer().getGameMode() == GameMode.SURVIVAL) {
                ItemStack usedItem = e.getPlayer().getInventory().getItem(e.getHand());
                if (usedItem != null) {
                    usedItem.setAmount(usedItem.getAmount() - 1);
                    e.getPlayer().getInventory().setItem(e.getHand(), usedItem);
                }
            }
        }
    }

    @EventHandler
    public void updateBlockState(BlockPhysicsEvent e) {
        if (e.isCancelled()) return;
//        System.out.println("Physics with " + e.getBlock().getType());
        if (e.getBlock().getType() == Material.AIR || e.getBlock().getType() == Material.CAVE_VINES || e.getBlock().getType() == Material.CAVE_VINES_PLANT) {
            Block below = e.getBlock().getRelative(BlockFace.DOWN);
            if (below.getType() == Material.CAVE_VINES) {
                CustomItem customItem = CustomBlockManager.getItemFromLeaves(below);
                if (customItem instanceof CustomCaveVineDirectional) {
                    e.setCancelled(true);
                    return;
                }
            }
//            Block top = e.getBlock().getRelative(BlockFace.UP);
//            if (top.getType() == Material.CAVE_VINES) {
//                CustomItem customItem = CustomBlockManager.getItemFromLeaves(top);
//                if (customItem instanceof CustomCaveVineDirectional) {
//                    e.setCancelled(true);
//                }
//            }
        }
    }

    @EventHandler
    public void onTreeGrowth(StructureGrowEvent e) {
        if (e.getSpecies() == TreeType.TREE || e.getSpecies() == TreeType.BIG_TREE) {
            var customItem = CustomBlockManager.getCustomItemFromBlock(e.getLocation().getBlock());
            if (customItem != null) {
                CustomBlockManager.breakFurniture(null, e.getLocation());
                if (customItem.getMechanics() != null) {
                    var structure = customItem.getMechanics().getSapling_structure();
                    if (structure != null) {
                        e.setCancelled(true);
                        GardensFurniture.structureHandler.spawnStructure(structure, e.getLocation());
                    }
                }
            } else {
                // vanilla sapling - remove dupe on growth
                    var display = CustomBlockManager.getDisplayEntity(e.getLocation());
                    if (display != null) {
                        if (GardensFurniture.itemRegistry.getCustomItem(display.getItemStack()) != null) {
                            display.remove();
                        }
                    }

            }
        }
    }
}
