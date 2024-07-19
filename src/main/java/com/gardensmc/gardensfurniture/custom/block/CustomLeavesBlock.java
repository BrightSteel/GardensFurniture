package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.events.CustomBlockPlaceEvent;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

public class CustomLeavesBlock extends CustomBlock {

    private final int distance;
    private final boolean persistent;

    private final String sapling;

    public CustomLeavesBlock(ConfigurationSection blockSection, String customBlockName) {
        super(blockSection);
        ConfigurationSection leavesSection = getConfigSectionOrThrow(blockSection, "leaves_data", customBlockName);
        this.distance = leavesSection.getInt("distance");
        this.persistent = leavesSection.getBoolean("persistent");
        this.sapling = blockSection.getString("sapling");
    }

    public int getDistance() {
        return distance;
    }

    public boolean isPersistent() {
        return persistent;
    }

    @Nullable
    public String getSapling() {
        return sapling;
    }

    @Override
    public boolean placeBlock(ItemStack heldItem, CustomPlaceableItem customItem, Player player, Location location, BlockFace blockFace, boolean persistent, FurnitureStore furnitureStore) {
        Block block = location.getBlock();
        block.setType(Material.SPRUCE_LEAVES);
        try {
            FurnitureStoreHandler.INSTANCE.addFurnitureStore(block, furnitureStore);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (block.getBlockData() instanceof Leaves leaves) {
            if (persistent) {
                leaves.setPersistent(true);
            }
            else {
                leaves.setDistance(7); // max for our decay detection
                leaves.setPersistent(false);
            }
            block.setBlockData(leaves);
        }

        Bukkit.getPluginManager().callEvent(new CustomBlockPlaceEvent(player, customItem, block));

        return true;
    }
}
