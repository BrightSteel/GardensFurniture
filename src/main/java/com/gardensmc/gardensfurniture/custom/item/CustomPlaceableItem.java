package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public abstract class CustomPlaceableItem extends CustomItem {

    protected final ConfigurationSection blockSection;
    private final List<String> placeTargets;
    private final List<Tag<Material>> placeTags;
    private final List<String> placeDirections;

    public CustomPlaceableItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        this.blockSection = itemSection.getConfigurationSection("block");
        this.placeTargets = blockSection.getStringList("place_targets")
                .stream()
                .map(String::toUpperCase)
                .toList();
        this.placeTags = blockSection.getStringList("place_tags")
                .stream()
                .map(s -> Bukkit.getTag("blocks", NamespacedKey.minecraft(s.toLowerCase()), Material.class))
                .toList();
        this.placeDirections = blockSection.getStringList("place_directions")
                .stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());
        if (placeDirections.contains("SIDES")) {
            placeDirections.addAll(List.of("NORTH", "SOUTH", "EAST", "WEST"));
        }
    }

    public void placeBlock(Location location) {
        placeBlock(null, null, location, new PlaceOptions().setForce(true));
    }

    public boolean placeBlock(Player player, ItemStack heldItem, Location location, PlaceOptions placeOptions) {
        if (placeOptions.isForce()) {
            return true;
        }
        return isValidBlockFace(player, placeOptions.getBlockFace(), location);
    }

    private boolean isValidBlockFace(Player player, BlockFace blockFace, Location location) {
        if (!placeDirections.isEmpty() && !placeDirections.contains(blockFace.toString().toUpperCase())) {
            return false;
        }
        var target = location.getBlock().getRelative(blockFace.getOppositeFace());
        if (!placeTargets.isEmpty()) {
            // first allow material
            if (placeTargets.contains(target.getType().toString().toUpperCase())) {
                return true;
            }
            // check for customItem
            var customItem = CustomBlockManager.getCustomItemFromBlock(target);
            return customItem != null && placeTargets.contains(customItem.getIdentifier().toUpperCase());
        }
        if (!placeTags.isEmpty()) {
            return placeTags.stream().anyMatch(materialTag -> materialTag.isTagged(target.getType()));
        }
        if (player == null && !getIdentifier().equals("log_mushroom") && !getIdentifier().contains("leaves")) { // world-gen, avoid placing in air
            return target.isSolid();
        }
        return true;
    }

    // used to get general properties of the block, if multiple blocks return main one
    public abstract CustomBlock getCustomBlock();
}
