package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.CustomBlockState;

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

    public boolean placeBlock(Player player, ItemStack heldItem, Location location, PlaceOptions placeOptions, Block clickedBlock) {
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
        return true;
    }

    // used to get general properties of the block, if multiple blocks return main one
    public abstract CustomBlock getCustomBlock();

    public List<CustomBlock> getPlaceableBlocks() {
        return List.of(getCustomBlock());
    }

    public CustomBlock getPlacedBlock(Block block) {
        return getCustomBlock();
    }

    public void onBuildGeyserBlockState(CustomBlockState.Builder builder, Block block) {
        getCustomBlock().onBuildGeyserBlockState(builder, block);
    }

    public void onBuildGeyserBlockData(CustomBlockData.Builder builder) {
        getCustomBlock().onBuildGeyserBlockData(builder);
    }

    public List<CustomBlockPermutation> getPermutations() {
        return getCustomBlock().getPermutations();
    }
}
