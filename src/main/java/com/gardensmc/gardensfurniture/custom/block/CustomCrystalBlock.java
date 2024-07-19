package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CustomCrystalBlock extends CustomItemFrameBlock {

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
        boolean success = super.placeBlock(heldItem, customItem, player, location, blockFace, persistent, furnitureStore);
        if (success) {
            // place amethyst crystal server-side to handle growth
            location.getBlock().setType(type.getCrystalMaterial());
        }
        return success;
    }
}


