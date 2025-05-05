package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomNoteBlock;
import org.bukkit.Location;
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
    public boolean placeBlock(Player player, ItemStack heldItem, Location location, PlaceOptions placeOptions) {
        if (!super.placeBlock(player, heldItem, location, placeOptions)) return false;
        CustomBlock customBlock;
        switch (placeOptions.getBlockFace()) {
                case UP, DOWN -> customBlock = getY();
                case EAST, WEST -> customBlock = getX();
                case NORTH, SOUTH -> customBlock = getZ();
                default -> throw new RuntimeException("Unknown block face: " + placeOptions.getBlockFace());
            }
        return customBlock.placeBlock(player, heldItem, this, location, placeOptions);
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
