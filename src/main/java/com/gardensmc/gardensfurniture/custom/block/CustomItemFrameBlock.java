package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.item.PlaceOptions;
import com.gardensmc.gardensfurniture.events.FurniturePlaceEvent;
import com.gardensmc.gardensfurniture.util.ItemFrameUtil;
import de.tr7zw.nbtapi.NBTEntity;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CustomItemFrameBlock extends CustomBlock {

    public CustomItemFrameBlock(ConfigurationSection blockSection) {
        super(blockSection);
    }

    @Override
    public boolean isSolid() {
        return hasBarrier();
    }

    @Override
    public boolean placeBlock(Player player, ItemStack heldItem, CustomPlaceableItem customItem, Location location, PlaceOptions placeOptions) {
        World world = location.getWorld();
        var blockFace = placeOptions.getBlockFace();
        if (world != null) {
            ItemFrame itemFrame = (ItemFrame) location.getWorld().spawnEntity(location, EntityType.ITEM_FRAME);
            itemFrame.setVisible(false);
            itemFrame.setFacingDirection(getFacingDirection(location, blockFace), true);
            ItemStack display = GardensFurniture.itemRegistry.buildItem(customItem);
            String custom_name = null;
            ItemMeta itemMeta = display.getItemMeta();
            if (itemMeta != null) {
                // remove name
                custom_name = itemMeta.getDisplayName();
                itemMeta.setDisplayName(null);

                // conserve color
                Color leatherColor = getColor(customItem, location, heldItem);
                if (leatherColor != null && itemMeta instanceof LeatherArmorMeta leatherArmorMeta) {
                    leatherArmorMeta.setColor(leatherColor);
                }

                display.setItemMeta(itemMeta);
            }
            itemFrame.setItem(display);
            if (blockFace == BlockFace.UP || blockFace == BlockFace.DOWN) {
                if (player != null) {
                    itemFrame.setRotation(ItemFrameUtil.getRotationFromFacing(player));
                } else {
                    var rotations = Rotation.values();
                    int rand = ThreadLocalRandom.current().nextInt(rotations.length);
                    itemFrame.setRotation(rotations[rand]);
                }
            }
            if (custom_name != null) {
                NBTEntity nbtEntity = new NBTEntity(itemFrame);
                nbtEntity.getPersistentDataContainer().setString("custom_name", custom_name);
            }

            if (hasBarrier()) {
                location.getBlock().setType(Material.BARRIER);
            }

            Bukkit.getPluginManager().callEvent(new FurniturePlaceEvent(null, customItem, itemFrame));

            return true;
        }
        return false;
    }

    private BlockFace getFacingDirection(Location location, BlockFace givenFace) {
        if (givenFace != null) {
            return givenFace;
        }
        // place on solid blockFace
        var block = location.getBlock();
        var facePriority = List.of(BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);
        for (BlockFace face : facePriority) {
            var relative = block.getRelative(face);
            if (!relative.getType().isAir() && relative.getType().isBlock() && relative.getType() != Material.ARMOR_STAND && relative.getType() != Material.ITEM_FRAME) {
                return face.getOppositeFace();
            }
        }
        return BlockFace.UP; // maybe fail instead ?
    }

    private Color getColor(CustomPlaceableItem customPlaceableItem, Location location, ItemStack heldItem) {
        // placing via terra - try to get item as nearest one of that type if in a patch
        if (heldItem == null) {
            ItemStack nearestItem = location.getWorld().getNearbyEntities(location, 2, 3, 2, e -> e instanceof ItemFrame)
                    .stream()
                    .map(entity -> ((ItemFrame) entity).getItem())
                    .filter(item -> {
                        var customItem = GardensFurniture.itemRegistry.getCustomItem(item);
                        return customItem != null && (
                                // ie crystal_large should work with crystal
                                customItem.getIdentifier().contains(customPlaceableItem.getIdentifier())
                                || customPlaceableItem.getIdentifier().contains(customItem.getIdentifier())
                        );
                    })
                    .findFirst().orElse(null);
            heldItem = nearestItem;
        }

        if (heldItem != null && heldItem.getItemMeta() instanceof LeatherArmorMeta originalLeather) {
            return originalLeather.getColor();
        } else {
            // random color
            return Color.fromRGB(ThreadLocalRandom.current().nextInt(0xFFFFFF));
        }
    }
}
