package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.item.PlaceOptions;
import com.gardensmc.gardensfurniture.events.CustomBlockPlaceEvent;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

public class CustomLeavesBlock extends CustomBlock {

    private final int distance;
    private final boolean persistent;

    private final String sapling;
    private final LeavesType leavesType;

    public CustomLeavesBlock(ConfigurationSection blockSection, String customBlockName, String blockId) {
        super(blockSection, blockId);
        ConfigurationSection leavesSection = getConfigSectionOrThrow(blockSection, "leaves_data", customBlockName);
        this.distance = leavesSection.getInt("distance");
        this.persistent = leavesSection.getBoolean("persistent");
        this.sapling = leavesSection.getString("sapling");
        this.leavesType = leavesSection.contains("type")
                ? LeavesType.valueOf(leavesSection.getString("type").toUpperCase())
                : LeavesType.SPRUCE;
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

    public LeavesType getLeavesType() {
        return leavesType;
    }

    @Override
    public boolean isSolid() {
        return true;
    }

    @Override
    public boolean placeBlock(Player player, ItemStack heldItem, CustomPlaceableItem customItem, Location location, PlaceOptions placeOptions, Block clickedBlock) {
        Block block = location.getBlock();
        block.setType(getLeavesType().getMaterial());
        FurnitureStoreHandler.INSTANCE.addFurnitureStore(block, new FurnitureStore(customItem.getIdentifier()), heldItem != null);
        if (block.getBlockData() instanceof Leaves leaves) {
            if (placeOptions.isPersistentLeaves()) {
                leaves.setPersistent(true);
            }
            else {
                leaves.setDistance(7); // max for our decay detection
                leaves.setPersistent(false);
            }
            block.setBlockData(leaves);
        }

        Bukkit.getPluginManager().callEvent(new CustomBlockPlaceEvent(player, customItem, block));
        // play place sound
        playPlaceSound(block, "vanilla.grass.place");

        return true;
    }

    public enum LeavesType {
        SPRUCE(Material.SPRUCE_LEAVES),
        CHERRY(Material.CHERRY_LEAVES),
        OAK(Material.OAK_LEAVES),
        BIRCH(Material.BIRCH_LEAVES);

        private final Material material;

        LeavesType(Material material) {
            this.material = material;
        }

        public Material getMaterial() {
            return material;
        }
    }
}
