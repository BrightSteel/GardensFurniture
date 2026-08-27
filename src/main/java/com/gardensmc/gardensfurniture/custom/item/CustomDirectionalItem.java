package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.CustomBlockPermutation;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CustomDirectionalItem extends CustomPlaceableItem {

    private CustomBlock x, y, z;

    public CustomDirectionalItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        this.x = GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("x"), identifier + "_x");
        this.y = GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("y"), identifier + "_y");
        this.z = GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("z"), identifier + "_z");
    }

    @Override
    public boolean placeBlock(Player player, ItemStack heldItem, Location location, PlaceOptions placeOptions, Block clickedBlock) {
        if (!super.placeBlock(player, heldItem, location, placeOptions, clickedBlock)) return false;
        CustomBlock customBlock = getCustomBlock(placeOptions.getBlockFace());
        return customBlock.placeBlock(player, heldItem, this, location, placeOptions.setFacing(placeOptions.getBlockFace().toString()), clickedBlock);
    }

    @Override
    public CustomBlock getCustomBlock() {
        return getX();
    }

    public CustomBlock getCustomBlock(BlockFace facing) {
        return switch (facing) {
            case UP, DOWN -> getY();
            case EAST, WEST -> getX();
            case NORTH, SOUTH -> getZ();
            default -> throw new RuntimeException("Unknown block face: " + facing) ;
        };
    }

    public CustomBlock getX() {
        return x;
    }

    public CustomBlock getY() {
        return y;
    }

    public CustomBlock getZ() {
        return z;
    }

    // only applicable for blocks with furniture store facing
    @Nullable
    public BlockFace getBlockDirection(Block block) {
        var furnitureStore = FurnitureStoreHandler.INSTANCE.getFurnitureStore(block);
        if (furnitureStore == null) {
            return null;
        }
        return BlockFace.valueOf(furnitureStore.getFacing());
    }

    @Override
    public List<CustomBlock> getPlaceableBlocks() {
        return List.of(x, y, z);
    }

    @Override
    public CustomBlock getPlacedBlock(Block block) {
        var blockFace = getBlockDirection(block);
        return switch (Objects.requireNonNull(blockFace)) {
            case UP, DOWN -> y;
            case EAST, WEST -> x;
            case NORTH, SOUTH -> z;
            default -> throw new RuntimeException("Unknown block face: " + blockFace);
        };
    }

    @Override
    public void onBuildGeyserBlockState(CustomBlockState.Builder builder, Block block) {
        var blockFace = getBlockDirection(block);
        switch (Objects.requireNonNull(blockFace)) {
            case UP, DOWN -> {
                builder.stringProperty("axis", "y");
                y.onBuildGeyserBlockState(builder, block);
            }
            case EAST, WEST -> {
                builder.stringProperty("axis", "x");
                x.onBuildGeyserBlockState(builder, block);
            }
            case NORTH, SOUTH -> {
                builder.stringProperty("axis", "z");
                z.onBuildGeyserBlockState(builder, block);
            }
            default -> throw new RuntimeException("Unknown block face: " + blockFace);
        }
    }

    @Override
    public List<CustomBlockPermutation> getPermutations() {
        var permutations = new ArrayList<CustomBlockPermutation>();
        if (y.getBedrock() != null) {
            permutations.add(new CustomBlockPermutation(y.getBedrock().toCustomBlockComponents(), "q.block_property('%s') == '%s'".formatted("axis", "y")));
        }
        if (x.getBedrock() != null) {
            permutations.add(new CustomBlockPermutation(x.getBedrock().toCustomBlockComponents(), "q.block_property('%s') == '%s'".formatted("axis", "x")));
        }
        if (z.getBedrock() != null) {
            permutations.add(new CustomBlockPermutation(z.getBedrock().toCustomBlockComponents(), "q.block_property('%s') == '%s'".formatted("axis", "z")));
        }
        permutations.addAll(x.getPermutations());
        permutations.addAll(y.getPermutations());
        permutations.addAll(z.getPermutations());
        return permutations;
    }

    @Override
    public void onBuildGeyserBlockData(CustomBlockData.Builder builder) {
        builder.stringProperty("axis", List.of("y", "x", "z"));
    }
}
