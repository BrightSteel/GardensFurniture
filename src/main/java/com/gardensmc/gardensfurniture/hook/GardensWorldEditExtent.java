package com.gardensmc.gardensfurniture.hook;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.type.NoteBlock;

public class GardensWorldEditExtent extends AbstractDelegateExtent {

    private final World world;

    public GardensWorldEditExtent(Extent extent, World world) {
        super(extent);
        this.world = world;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) throws WorldEditException {
        var blockData = BukkitAdapter.adapt(block);
        var location = new Location(world, x, y, z);
//        if (blockData instanceof NoteBlock noteBlock) {
//            var key = noteBlock.getNote().getId() + "," + noteBlock.getInstrument() + "," + noteBlock.isPowered();
//            var customItem = GardensFurniture.itemRegistry.getCustomItem(key);
//            if (customItem instanceof CustomPlaceableItem placeable) {
//                furnitureAction(() -> {
//                    location.getBlock().setType(Material.AIR);
//                    placeable.placeBlock(location);
//                });
//            }
//        }
        return super.setBlock(x, y, z, block);
    }

    private void furnitureAction(Runnable action) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(GardensFurniture.plugin, action, 1);
    }

    private void print(String m) {
        Bukkit.getScheduler().runTask(GardensFurniture.plugin, () -> System.out.println(m));
    }
}
