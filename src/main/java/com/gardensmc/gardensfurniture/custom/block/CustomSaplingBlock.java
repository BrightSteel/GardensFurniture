package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.item.PlaceOptions;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Getter
public class CustomSaplingBlock extends CustomDisplayBlock {

    private SaplingType saplingType = SaplingType.SMALL;
    private final String identifier;

    public CustomSaplingBlock(ConfigurationSection blockSection, String identifier) {
        super(blockSection, identifier);
        var saplingSection = blockSection.getConfigurationSection("sapling_data");
        if (saplingSection != null) {
            var saplingTypeStr = saplingSection.getString("type");
            if (saplingTypeStr != null) {
                this.saplingType = SaplingType.valueOf(saplingTypeStr.toUpperCase());
            }
        }
        this.identifier = identifier;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public boolean placeBlock(Player player, ItemStack heldItem, CustomPlaceableItem customItem, Location location, PlaceOptions placeOptions) {
        var furnitureStore = new FurnitureStore(customItem.getIdentifier());
        FurnitureStoreHandler.INSTANCE.addFurnitureStore(location.getBlock(), furnitureStore, heldItem != null);
        var modelItem = GardensFurniture.itemRegistry.buildItem(identifier + "_model");
        this.placeDisplay(customItem, modelItem != null ? modelItem : heldItem, player, location, placeOptions.getBlockFace());
        return false; // handle placement naturally with event
    }

    @AllArgsConstructor
    @Getter
    public enum SaplingType {
        SMALL(Material.OAK_SAPLING),
        LARGE(Material.DARK_OAK_SAPLING);

        private final Material material;

    }
}
