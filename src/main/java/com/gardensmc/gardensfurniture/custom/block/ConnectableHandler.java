package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.type.Connectable;
import com.gardensmc.gardensfurniture.custom.type.Facing;
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
        var left = getAttachedBlock(Facing.fromBlockFace(facing).clockwise().toBlockFace());
        var right = getAttachedBlock(Facing.fromBlockFace(facing.getOppositeFace()).clockwise().toBlockFace());
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
}
