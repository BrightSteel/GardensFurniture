package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.type.Connectable;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PaperBlockItemDataProperties;
import lombok.AllArgsConstructor;
import net.minecraft.world.item.component.BlockItemStateProperties;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@AllArgsConstructor
public class ConnectableHandler {

    private final ItemDisplay display;
    private final CustomItem customItem;

    public void updateConnectableState() {
        var state = getConnectableState();
        var item = display.getItemStack();
        item.setData(DataComponentTypes.BLOCK_DATA, new PaperBlockItemDataProperties(new BlockItemStateProperties(
                Map.of("connectable", state.toString().toLowerCase())
        )));
        display.setItemStack(item);
    }

    private Connectable getConnectableState() {
        BlockFace facing = display.getFacing();
        var behind = getAttachedBlock(facing.getOppositeFace());
        if (behind != null) {
            return Connectable.OUTER;
        }
        var front = getAttachedBlock(facing);
        if (front != null) {
            return Connectable.INNER;
        }
        var left = getAttachedBlock(offsetClockwise(facing));
        var right = getAttachedBlock(offsetClockwise(facing.getOppositeFace()));
        if (left == null && right == null) {
            return Connectable.STRAIGHT;
        }
        if (left == null) {
            return Connectable.LEFT;
        }
        return Connectable.RIGHT;
    }

    @Nullable
    private ItemDisplay getAttachedBlock(BlockFace blockFace) {
        var block = display.getLocation().getBlock();
        var relative = block.getRelative(blockFace);
        var relativeDisplay = CustomBlockManager.getDisplayEntity(relative.getLocation());
        if (relativeDisplay == null) {
            return null;
        }
        var attachedItem = GardensFurniture.itemRegistry.getCustomItem(relativeDisplay.getItemStack());
        if (attachedItem == null || !attachedItem.equals(customItem)) {
            return null;
        }
        return relativeDisplay;
    }

    private BlockFace offsetClockwise(BlockFace origin) {
        return switch (origin) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> throw new RuntimeException("Illegal rotation origin: " + origin);
        };
    }

    private BlockFace offsetCounterClockwise(BlockFace origin) {
        return switch (origin) {
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            default -> throw new RuntimeException("Illegal rotation origin: " + origin);
        };
    }
}
