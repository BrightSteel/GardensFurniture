package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.custom.item.CustomCaveVineDirectional;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import com.gardensmc.gardensfurniture.util.DirectionUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

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
    public boolean placeBlock(ItemStack heldItem, CustomPlaceableItem customItem, Player player, Location location, BlockFace blockFace, boolean persistent, FurnitureStore furnitureStore) {
        if (heldItem != null && customItem.getMaterial() == Material.GLOW_BERRIES) return false; // place naturally (if not placed by terrain mapping)

        Block block = location.getBlock();
        FurnitureStoreHandler.INSTANCE.addFurnitureStore(block, furnitureStore, heldItem != null);
        block.setType(Material.CAVE_VINES, true);
        return true;
    }
}
