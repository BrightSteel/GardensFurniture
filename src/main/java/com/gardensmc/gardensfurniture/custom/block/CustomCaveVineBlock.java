package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.item.PlaceOptions;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Getter
public class CustomCaveVineBlock extends CustomBlock {

    private final int age;
    // growth
    private final boolean growBerries;
    private final boolean grow;
    private final double growthRate;

    public CustomCaveVineBlock(ConfigurationSection blockSection, String customBlockName, String blockId) {
        super(blockSection, blockId);
        // walked through on Java, so on Bedrock too
        if (getBedrock() != null) getBedrock().setPassable(true);
        ConfigurationSection caveVineSection = getConfigSectionOrThrow(blockSection, "cave_vine_data", customBlockName);
        this.age = caveVineSection.getInt("age");
        this.growBerries = blockSection.getBoolean("grow_berries", false);
        this.grow = blockSection.getBoolean("grow");
        this.growthRate = blockSection.getDouble("growth_rate", 100);
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public boolean placeBlock(Player player, ItemStack heldItem, CustomPlaceableItem placeableItem, Location location, PlaceOptions placeOptions, Block clickedBlock) {
        Block block = location.getBlock();
        FurnitureStoreHandler.INSTANCE.addFurnitureStore(block, new FurnitureStore(placeableItem.getIdentifier()), heldItem != null);
        if (heldItem != null && placeableItem.getMaterial() == Material.GLOW_BERRIES) {
            if (placeOptions.getBlockFace() == BlockFace.DOWN && block.getType().toString().toUpperCase().endsWith("LEAVES")) {
                block.setType(Material.CAVE_VINES, true);
                return true;
            }
            FurnitureStoreHandler.INSTANCE.removeIfNotPlaced(block, type -> type == Material.CAVE_VINES || type == Material.CAVE_VINES_PLANT);
            return false; // place naturally (if not placed by terrain mapping)
        }
        block.setType(Material.CAVE_VINES, true);
        // play place sound
        playPlaceSound(block, "block.cave_vines.place");

        return true;
    }
}