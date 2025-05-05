package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.item.PlaceOptions;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CustomCaveVineBlock extends CustomBlock {

    private final int age;
    private final boolean allowGlow;

    public CustomCaveVineBlock(ConfigurationSection blockSection, String customBlockName) {
        super(blockSection);
        ConfigurationSection caveVineSection = getConfigSectionOrThrow(blockSection, "cave_vine_data", customBlockName);
        this.age = caveVineSection.getInt("age");
        this.allowGlow = caveVineSection.getBoolean("allow_glow");
    }

    public int getAge() {
        return age;
    }

    public boolean allowGlow() {
        return allowGlow;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public boolean placeBlock(Player player, ItemStack heldItem, CustomPlaceableItem customItem, Location location, PlaceOptions placeOptions) {
        if (heldItem != null && customItem.getMaterial() == Material.GLOW_BERRIES) return false; // place naturally (if not placed by terrain mapping)

        Block block = location.getBlock();
        FurnitureStoreHandler.INSTANCE.addFurnitureStore(block, new FurnitureStore(customItem.getIdentifier()), heldItem != null);
        block.setType(Material.CAVE_VINES, true);
        return true;
    }
}
