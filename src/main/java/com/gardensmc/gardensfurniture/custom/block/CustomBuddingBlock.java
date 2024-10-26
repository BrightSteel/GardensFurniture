package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.events.CustomBlockPlaceEvent;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CustomBuddingBlock extends CustomNoteBlock {

    private final String crystalId;

    public CustomBuddingBlock(ConfigurationSection blockSection, String blockName) {
        super(blockSection, blockName);
        this.crystalId = blockSection.getString("crystal_id");
    }

    public String getCrystalId() {
        return crystalId;
    }

    @Override
    public boolean placeBlock(ItemStack heldItem, CustomPlaceableItem customItem, Player player, Location location, BlockFace blockFace, boolean persistent, FurnitureStore furnitureStore) {
        Block block = location.getBlock();
        CustomBlockPlaceEvent event = new CustomBlockPlaceEvent(player, customItem, block);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        block.setType(Material.BUDDING_AMETHYST);
        boolean updateCache = true;
        if (heldItem == null) {
            updateCache = false;
            // placed by terra, lets do chance of spawning some buds
            for (BlockFace face : List.of(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
                if (ThreadLocalRandom.current().nextInt(2) == 0) {
                    var b = block.getRelative(face);
                    if (b.getType() == Material.AIR) {
                        CustomPlaceableItem placeable = (CustomPlaceableItem) GardensFurniture.itemRegistry.getItem(
                                GardensFurniture.itemRegistry.crystalRegistry.getCrystalIdentifier(crystalId, CustomCrystalBlock.CrystalType.getRandom())
                        );
                        CustomBlockManager.attemptPlacement(null, null, placeable, b.getLocation(), face, false);
                    }
                }
            }
        }
        FurnitureStoreHandler.INSTANCE.addFurnitureStore(block, furnitureStore, updateCache);

        return true;
    }
}
