package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomNoteBlock;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CustomDirectionalItem extends CustomPlaceableItem {

    private CustomBlock x, y, z;

    public CustomDirectionalItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        this.x = GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("x"), identifier + "_x");
        this.y = GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("y"), identifier + "_y");
        this.z = GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("z"), identifier + "_z");
    }

    @Override
    public boolean placeBlock(Player player, ItemStack heldItem, Location location, PlaceOptions placeOptions) {
        if (!super.placeBlock(player, heldItem, location, placeOptions)) return false;
        CustomBlock customBlock = getCustomBlock(placeOptions.getBlockFace());
        return customBlock.placeBlock(player, heldItem, this, location, placeOptions.setFacing(placeOptions.getBlockFace().toString()));
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
}
