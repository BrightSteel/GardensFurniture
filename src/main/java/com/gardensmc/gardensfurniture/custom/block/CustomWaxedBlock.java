package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.item.PlaceOptions;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

@Getter
public class CustomWaxedBlock extends CustomBlock {

    private final WaxedType waxedType;

    public CustomWaxedBlock(ConfigurationSection blockSection, String identifier) {
        super(blockSection);
        var waxedSection = blockSection.getConfigurationSection("waxed_data");
        if (waxedSection == null) {
            throw new IllegalStateException("Missing waxed_data for block " + identifier);
        }
        var waxedTypeStr = waxedSection.getString("type");
        if (waxedTypeStr == null) {
            throw new IllegalStateException("Missing waxed type for " + identifier);
        }
        var waxedVariation = waxedSection.getString("variation");
        if (waxedVariation == null) {
            throw new IllegalStateException("Missing waxed variation for " + identifier);
        }
        this.waxedType = WaxedType.valueOf((waxedTypeStr + "_" + waxedVariation).toUpperCase());
    }

    @Override
    public boolean isSolid() {
        return true;
    }

    @Override
    public boolean placeBlock(Player player, ItemStack heldItem, CustomPlaceableItem customItem, Location location, PlaceOptions placeOptions) {
        var furnitureStore = new FurnitureStore(customItem.getIdentifier());
        FurnitureStoreHandler.INSTANCE.addFurnitureStore(location.getBlock(), furnitureStore, heldItem != null);
        if (waxedType.toString().startsWith("DOOR")) {
            // top door block
            FurnitureStoreHandler.INSTANCE.addFurnitureStore(location.getBlock().getRelative(BlockFace.UP), furnitureStore, heldItem != null);
        }
        return false; // allow natural placement
    }

    @Getter
    @AllArgsConstructor
    public enum WaxedType {
        STAIRS_NORMAL(Material.WAXED_CUT_COPPER_STAIRS),
        SLAB_NORMAL(Material.WAXED_CUT_COPPER_SLAB),
        DOOR_NORMAL(Material.WAXED_COPPER_DOOR),
        TRAPDOOR_NORMAL(Material.WAXED_COPPER_TRAPDOOR),

        STAIRS_EXPOSED(Material.WAXED_EXPOSED_CUT_COPPER_STAIRS),
        SLAB_EXPOSED(Material.WAXED_EXPOSED_CUT_COPPER_SLAB),
        DOOR_EXPOSED(Material.WAXED_EXPOSED_COPPER_DOOR),
        TRAPDOOR_EXPOSED(Material.WAXED_EXPOSED_COPPER_TRAPDOOR),

        STAIRS_WEATHERED(Material.WAXED_WEATHERED_CUT_COPPER_STAIRS),
        SLAB_WEATHERED(Material.WAXED_WEATHERED_CUT_COPPER_SLAB),
        DOOR_WEATHERED(Material.WAXED_WEATHERED_COPPER_DOOR),
        TRAPDOOR_WEATHERED(Material.WAXED_WEATHERED_COPPER_TRAPDOOR),

        STAIRS_OXIDIZED(Material.WAXED_OXIDIZED_CUT_COPPER_STAIRS),
        SLAB_OXIDIZED(Material.WAXED_OXIDIZED_CUT_COPPER_SLAB),
        DOOR_OXIDIZED(Material.WAXED_OXIDIZED_COPPER_DOOR),
        TRAPDOOR_OXIDIZED(Material.WAXED_OXIDIZED_COPPER_TRAPDOOR);

        public static List<Material> materials() {
            return Arrays.stream(values()).map(WaxedType::getMaterial).toList();
        }

        private final Material material;
    }
}
