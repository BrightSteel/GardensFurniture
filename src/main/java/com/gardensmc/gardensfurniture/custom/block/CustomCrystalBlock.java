package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CustomCrystalBlock extends CustomDisplayBlock {

    private final String crystalId;
    private final CrystalType type;

    public CustomCrystalBlock(ConfigurationSection blockSection, String customBlockName) {
        super(blockSection);
        var crystalSection = blockSection.getConfigurationSection("crystal_data");
        if (crystalSection == null) {
            throw new RuntimeException("Missing crystal section for block: " + customBlockName);
        }
        this.crystalId = crystalSection.getString("id");
        this.type = CrystalType.valueOf(crystalSection.getString("type").toUpperCase());
    }

    public CrystalType getType() {
        return type;
    }

    public String getCrystalId() {
        return crystalId;
    }

    public enum CrystalType {
        SMALL_BUD(Material.SMALL_AMETHYST_BUD),
        MEDIUM_BUD(Material.MEDIUM_AMETHYST_BUD),
        LARGE_BUD(Material.LARGE_AMETHYST_BUD),
        CLUSTER(Material.AMETHYST_CLUSTER);

        private final Material crystalMaterial;

        CrystalType(Material crystalMaterial) {
            this.crystalMaterial = crystalMaterial;
        }

        public Material getCrystalMaterial() {
            return crystalMaterial;
        }
    }

    @Override
    public boolean placeBlock(ItemStack heldItem, CustomPlaceableItem customItem, Player player, Location location, BlockFace blockFace, boolean persistent, FurnitureStore furnitureStore) {
        if (heldItem != null && isCrystalMaterial(heldItem.getType())) {
            return false; // handle placement naturally w/ event
        }
        boolean success = placeDisplayEntity(heldItem, customItem, player, location, blockFace, persistent, furnitureStore);
        if (success) {
            var block = location.getBlock();
            FurnitureStoreHandler.INSTANCE.addFurnitureStore(block, furnitureStore);
            // place amethyst crystal server-side to handle growth
            block.setType(type.getCrystalMaterial());
        }
        return success;
    }

    public boolean placeDisplayEntity(ItemStack heldItem, CustomPlaceableItem customItem, Player player, Location location, BlockFace blockFace, boolean persistent, FurnitureStore furnitureStore) {
        return super.placeBlock(heldItem, customItem, player, location, blockFace, persistent, furnitureStore);
    }

    public static boolean isCrystalMaterial(Material material) {
        return material == Material.SMALL_AMETHYST_BUD
                || material == Material.MEDIUM_AMETHYST_BUD
                || material == Material.LARGE_AMETHYST_BUD
                || material == Material.AMETHYST_CLUSTER;
    }
}


