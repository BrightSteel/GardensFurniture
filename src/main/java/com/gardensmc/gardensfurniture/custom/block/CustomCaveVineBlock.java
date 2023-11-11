package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.custom.item.CustomCaveVineDirectional;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.util.DirectionUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class CustomCaveVineBlock extends CustomBlock {

    private final int age;

    public CustomCaveVineBlock(ConfigurationSection blockSection, String customBlockName) {
        super(blockSection);
        ConfigurationSection caveVineSection = getConfigSectionOrThrow(blockSection, "cave_vine_data", customBlockName);
        this.age = caveVineSection.getInt("age");
    }

    public int getAge() {
        return age;
    }

    @Override
    public boolean placeBlock(CustomPlaceableItem customItem, Player player, Location location, BlockFace blockFace, boolean persistent) {
        if (customItem.getMaterial() == Material.GLOW_BERRIES) return false; // place naturally

        String identifier = customItem.getIdentifier();

        if (customItem instanceof CustomCaveVineDirectional) {
            String direction = DirectionUtil.getDirectionFromFacing(player);
            identifier += "." + direction;
        }

        Block block = location.getBlock();
        FurnitureStore.INSTANCE.addFurnitureStore(block, identifier);
        block.setType(Material.CAVE_VINES, true);
        return true;
    }
}
