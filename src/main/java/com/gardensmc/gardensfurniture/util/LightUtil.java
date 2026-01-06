package com.gardensmc.gardensfurniture.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.ItemDisplay;

import java.util.Objects;
import java.util.Set;

public class LightUtil {

    private static final Set<BlockFace> blockFaces = Set.of(
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
    );

    public static void placeLight(Location location, int level, boolean isSolid) {
        if (isSolid) {
            for (var blockFace : blockFaces) {
                var block = location.getBlock().getRelative(blockFace);
                if (block.getType() == Material.AIR) {
                    block.setType(Material.LIGHT);
                    Light lightBlock = (Light) block.getBlockData();
                    lightBlock.setLevel(level);
                }
            }
        } else {
            var block = location.getBlock();
            block.setType(Material.LIGHT);
            Light lightBlock = (Light) block.getBlockData();
            lightBlock.setLevel(level);
        }
    }

    public static void removeLight(Location location, boolean isSolid) {
        if (isSolid) {
            for (var blockFace : blockFaces) {
                var block = location.getBlock().getRelative(blockFace);
                if (block.getType() == Material.LIGHT) {
                    block.setType(Material.AIR);
                }
            }
        } else {
            var block = location.getBlock();
            block.setType(Material.AIR);
        }
    }

    public static void addLightBack(Block block) {
        var light = LightUtil.getCoveringLight(block);
        if (light > 0) {
            block.setType(Material.LIGHT);
            ((Light) block.getBlockData()).setLevel(light);
        }
    }

    private static int getCoveringLight(Block block) {
        for (var blockFace : blockFaces) {
            var relative = block.getRelative(blockFace);
            var customBlock = CustomBlockManager.getCustomItemFromBlock(relative);
            if (customBlock instanceof CustomPlaceableItem placeable && placeable.getCustomBlock().isSolid()) {
                var light = placeable.getCustomBlock().getLight();
                if (light > 0) {
                    return light;
                }
            }
        }

        // account for displays with light offsets
        return block.getLocation().getNearbyEntitiesByType(ItemDisplay.class, 5)
                .stream()
                .map(itemDisplay -> {
                    var customItem = GardensFurniture.itemRegistry.getCustomItem(itemDisplay.getItemStack());
                    if (customItem instanceof CustomPlaceableItem placeable) {
                        var lightOffset = placeable.getCustomBlock().getLightOffset();
                        if (lightOffset > 0 && itemDisplay.getLocation().toBlockLocation().add(0, lightOffset, 0).toVector().equals(block.getLocation().toVector())) {
                            return placeable.getCustomBlock().getLight();
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(0);
    }
}
