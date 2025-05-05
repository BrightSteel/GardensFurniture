package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.item.PlaceOptions;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.AmethystCluster;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class CustomCrystalBlock extends CustomDisplayBlock {

    private final String crystalId;
    private final CrystalType type;
    private final String modelOverride;

    public CustomCrystalBlock(ConfigurationSection blockSection, String customBlockName) {
        super(blockSection, customBlockName);
        this.hitbox = Hitbox.NONE;
        var crystalSection = blockSection.getConfigurationSection("crystal_data");
        if (crystalSection == null) {
            throw new RuntimeException("Missing crystal section for block: " + customBlockName);
        }
        this.crystalId = crystalSection.getString("id");
        this.type = CrystalType.valueOf(crystalSection.getString("type").toUpperCase());
        this.modelOverride = crystalSection.getString("model_override");
    }

    public CrystalType getType() {
        return type;
    }

    public String getCrystalId() {
        return crystalId;
    }

    public String getModelOverride() {
        return modelOverride;
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

        @Nullable
        public static CrystalType fromMaterial(Material material) {
            return Arrays.stream(CrystalType.values()).filter(crystalType -> crystalType.getCrystalMaterial() == material)
                    .findFirst()
                    .orElse(null);
        }

        public static CrystalType getRandom() {
            var values = CrystalType.values();
            var r = ThreadLocalRandom.current().nextInt(values.length);
            return CrystalType.values()[r];
        }
    }

    @Override
    public boolean placeBlock(Player player, ItemStack heldItem, CustomPlaceableItem customItem, Location location, PlaceOptions placeOptions) {
        if (heldItem != null && isCrystalMaterial(heldItem.getType())) {
            return false; // handle placement naturally w/ event
        }
        var blockFace = placeOptions.getBlockFace();
        boolean success = placeDisplayEntity(customItem, location, blockFace);
        if (success) {
            var block = location.getBlock();
            // place amethyst crystal server-side to handle growth & hitbox
            block.setType(type.getCrystalMaterial());
            if (block.getBlockData() instanceof AmethystCluster cluster) {
                cluster.setFacing(blockFace);
                block.setBlockData(cluster);
            }
        }
        return success;
    }

    public boolean placeDisplayEntity(CustomPlaceableItem customItem, Location location, BlockFace blockFace) {
        var crystalItem = GardensFurniture.itemRegistry.buildItem(customItem);
        ItemMeta meta = crystalItem.getItemMeta();
        if (meta != null && !modelOverride.isEmpty()) {
            meta.setItemModel(NamespacedKey.fromString(modelOverride));
            crystalItem.setItemMeta(meta);
        }
        return super.placeDisplay(crystalItem, null, location, blockFace);
    }

    public static boolean isCrystalMaterial(Material material) {
        return material == Material.SMALL_AMETHYST_BUD
                || material == Material.MEDIUM_AMETHYST_BUD
                || material == Material.LARGE_AMETHYST_BUD
                || material == Material.AMETHYST_CLUSTER;
    }
}


