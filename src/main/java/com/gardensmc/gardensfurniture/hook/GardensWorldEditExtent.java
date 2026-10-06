package com.gardensmc.gardensfurniture.hook;

import com.fastasyncworldedit.core.nbt.FaweCompoundTag;
import com.gardensmc.gardensfurniture.store.FurnitureCache;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Hands stored furniture blocks out as proxies when an edit reads them (copying, cutting, moving), so their
 * store travels with them. See {@link WorldEditHook}. FAWE drops this extent unless its class name is under
 * {@code extent.allowed-plugins} in FAWE's config.yml - keep the name stable.
 */
public class GardensWorldEditExtent extends AbstractDelegateExtent {

    private final String world;
    // null without a FAWE queue to run it (fast-placement off): writes are handled here instead
    @Nullable
    private final FurnitureEditProcessor processor;

    public GardensWorldEditExtent(Extent extent, String world, @Nullable FurnitureEditProcessor processor) {
        super(extent);
        this.world = world;
        this.processor = processor;
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return getFullBlock(position.x(), position.y(), position.z());
    }

    @Override
    public BaseBlock getFullBlock(int x, int y, int z) {
        var block = super.getFullBlock(x, y, z);
        if (!WorldEditHook.isStoreBacked(block.getBlockType())) {
            return block;
        }
        var store = FurnitureCache.INSTANCE.getFurnitureStore(
                new FurnitureCache.ChunkKey(world, x >> 4, z >> 4),
                new FurnitureCache.BlockKey(x, y, z));
        return store == null ? block : WorldEditHook.toProxy(block.toImmutableState(), store);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block) throws WorldEditException {
        return setBlock(position.x(), position.y(), position.z(), block);
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(int x, int y, int z, T block) throws WorldEditException {
        if (processor != null) {
            return super.setBlock(x, y, z, block);
        }
        var furniture = block instanceof BaseBlock base ? WorldEditHook.fromProxy(base.toImmutableState(), base.getNbt()) : null;
        if (furniture != null) {
            WorldEditHook.apply(world, List.of(new WorldEditHook.StoreChange(x, y, z, furniture.store())), null);
            return super.setBlock(x, y, z, furniture.state());
        }
        var stores = FurnitureCache.INSTANCE.getCachedChunkFurniture(new FurnitureCache.ChunkKey(world, x >> 4, z >> 4));
        if (stores == null || stores.containsKey(new FurnitureCache.BlockKey(x, y, z))) {
            WorldEditHook.apply(world, List.of(new WorldEditHook.StoreChange(x, y, z, null)), null);
        }
        return super.setBlock(x, y, z, block);
    }

    // undo and redo hand block entities back here before restoring blocks - ours carry a store, and go
    // nowhere near the world (FAWE's default would put the NBT on whatever block is there right now)
    @Override
    public boolean tile(int x, int y, int z, FaweCompoundTag tile) throws WorldEditException {
        var store = tile == null ? null : WorldEditHook.storeOf(tile.linTag());
        if (store == null) {
            return super.tile(x, y, z, tile);
        }
        if (processor != null) {
            processor.restore(x, y, z, store);
        } else {
            WorldEditHook.apply(world, List.of(new WorldEditHook.StoreChange(x, y, z, store)), null);
        }
        return true;
    }
}
