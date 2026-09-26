package com.gardensmc.gardensfurniture.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.block.CustomDisplayBlock;
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
                setLight(location.getBlock().getRelative(blockFace), level);
            }
        } else {
            setLight(location.getBlock(), level);
        }
    }

    // only ever fills empty space - a light offset must never replace a real block
    private static void setLight(Block block, int level) {
        var type = block.getType();
        if (type != Material.AIR && type != Material.CAVE_AIR && type != Material.LIGHT) {
            return;
        }
        Light lightBlock = (Light) Material.LIGHT.createBlockData();
        lightBlock.setLevel(level);
        block.setBlockData(lightBlock);
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
            if (block.getType() == Material.LIGHT) {
                block.setType(Material.AIR);
            }
        }
    }

    public static void addLightBack(Block block) {
        var light = LightUtil.getCoveringLight(block);
        if (light > 0) {
            setLight(block, light);
        }
    }

    private static int getCoveringLight(Block block) {
        for (var blockFace : blockFaces) {
            var relative = block.getRelative(blockFace);
            var customBlock = CustomBlockManager.getCustomItemFromBlock(relative);
            if (customBlock instanceof CustomPlaceableItem placeable && placeable.getCustomBlock().isSolid()) {
                var light = placeable.getCustomBlock().getLight();
                if (light > 0 && isLightOn(placeable, CustomBlockManager.getDisplayEntity(relative.getLocation(), true))) {
                    return light;
                }
            }
        }

        // account for displays with light offsets - offsets are vertical, so only this block's column can
        // hold one (runs a tick after every block break, so keep the search small)
        return block.getLocation().add(0.5, 0, 0.5).getNearbyEntitiesByType(ItemDisplay.class, 0.5, 6, 0.5)
                .stream()
                .map(itemDisplay -> {
                    var customItem = GardensFurniture.itemRegistry.getCustomItem(itemDisplay.getItemStack());
                    if (customItem instanceof CustomPlaceableItem placeable) {
                        var lightOffset = placeable.getCustomBlock().getLightOffset();
                        if (lightOffset > 0 && isLightOn(placeable, itemDisplay) && itemDisplay.getLocation().toBlockLocation().add(0, lightOffset, 0).toVector().equals(block.getLocation().toVector())) {
                            return placeable.getCustomBlock().getLight();
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(0);
    }

    // toggleable displays that are switched off shouldn't have their light restored
    private static boolean isLightOn(CustomPlaceableItem placeable, ItemDisplay display) {
        if (placeable.getCustomBlock() instanceof CustomDisplayBlock displayBlock && displayBlock.isLightToggleable()) {
            return display != null && displayBlock.isLightOn(display);
        }
        return true;
    }
}
