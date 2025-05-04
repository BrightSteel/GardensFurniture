package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomNoteBlock;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


public class CustomDirectionalItem extends CustomPlaceableItem {

    private CustomBlock x, y, z;

    public CustomDirectionalItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        this.x = GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("x"));
        this.y = GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("y"));
        this.z = GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, blockSection.getConfigurationSection("z"));
    }

    @Override
    public boolean placeBlock(ItemStack heldItem, Player player, Location location, BlockFace blockFace, boolean persistent) {
        if (!super.placeBlock(heldItem, player, location, blockFace, persistent)) return false;
        CustomBlock customBlock;
        switch (blockFace) {
                case UP, DOWN -> customBlock = getY();
                case EAST, WEST -> customBlock = getX();
                case NORTH, SOUTH -> customBlock = getZ();
                default -> throw new RuntimeException("Unknown block face: " + blockFace);
            }
        return customBlock.placeBlock(heldItem, this, player, location, blockFace, persistent, new FurnitureStore(getIdentifier()));
    }

    @Override
    public CustomBlock getCustomBlock() {
        return getX();
    }

    public CustomBlock getCustomBlock(String direction) {
        return switch (direction) {
            case "x" -> x;
            case "y" -> y;
            case "z" -> z;
            default -> null;
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

    // only applicable for CustomNoteBlocks, otherwise null
    @Nullable
    public String getBlockDirection(NoteBlock noteBlock) {
        return Map.of(
                "x", x,
                "y", y,
                "z", z
        ).entrySet()
                .stream()
                .filter(entry -> entry.getValue() instanceof CustomNoteBlock entryNoteBlock && entryNoteBlock.isNoteBlock(noteBlock))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
