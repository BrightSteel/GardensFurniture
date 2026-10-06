package com.gardensmc.gardensfurniture.hook;

import com.fastasyncworldedit.core.queue.IBlocks;
import com.fastasyncworldedit.core.queue.IQueueExtent;
import com.fastasyncworldedit.core.util.ExtentTraverser;
import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import com.fastasyncworldedit.core.registry.state.PropertyKey;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinIntTag;
import org.enginehub.linbus.tree.LinStringTag;
import org.enginehub.linbus.tree.LinTagType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Keeps store-backed furniture (note block, tripwire, leaves, cave vine, waxed, sapling) through FAWE's
 * //copy, //cut, //move, //stack, //paste and schematics.
 *
 * <p>That furniture is a plain vanilla block plus a {@link FurnitureStore} in its chunk's data, keyed by
 * position, so an edit moves the block and leaves the store behind. FAWE carries block entity NBT through
 * clipboards, rotations and schematic files, but only on blocks that have a block entity - so reads
 * ({@link GardensWorldEditExtent}) hand each stored block out as a barrel holding the real block state and
 * the store, and writes ({@link FurnitureEditProcessor}) turn those barrels back into the real block and
 * write its store. The same processor drops the store of every stored block an edit overwrites (//set,
 * //cut, //undo, ...), so it can't disguise whatever ends up there, and saves it in the edit's history as a
 * removed block entity so an //undo puts it back.</p>
 *
 * <p>Display furniture is entities plus barrier and light blocks, which FAWE copies itself with -e.</p>
 *
 * <p>FAWE only keeps a third-party extent whose class is listed under {@code extent.allowed-plugins} in its
 * config.yml, so {@link GardensWorldEditExtent} has to be listed there.</p>
 */
public class WorldEditHook {

    static final String PROXY_KEY = "GardensFurniture";

    // written facing north: whichever way a placed proxy faces is how far the edit turned the clipboard
    private static final BlockState PROXY = BlockTypes.BARREL.getDefaultState().with(PropertyKey.FACING, Direction.NORTH);
    private static final List<AffineTransform> TURNS = IntStream.of(0, 90, 180, 270)
            .mapToObj(degrees -> new AffineTransform().rotateY(degrees))
            .toList();

    // indexed by BlockType internal id
    private static boolean[] storeBacked = new boolean[0];

    public void register() {
        var table = new boolean[BlockTypesCache.values.length];
        for (var material : CustomBlockManager.storeBackedTypes()) {
            var type = BlockTypes.get(material.getKey().asString());
            if (type != null) {
                table[type.getInternalId()] = true;
            }
        }
        storeBacked = table;
        WorldEdit.getInstance().getEventBus().register(this);
        GardensFurniture.plugin.getLogger().info("Hooked into FastAsyncWorldEdit - furniture blocks survive copy, paste and schematics.");
    }

    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        // BEFORE_CHANGE sits right on the world queue, under history, so undo goes through it too
        if (event.getStage() != EditSession.Stage.BEFORE_CHANGE || event.getWorld() == null) {
            return;
        }
        String world = event.getWorld().getName();
        Extent extent = event.getExtent();
        // the processor sees every chunk an edit writes, including bulk sets that skip extents' setBlock;
        // without a queue (fast-placement off) the extent handles writes block by block instead
        IQueueExtent<?> queue = new ExtentTraverser<>(extent).findAndGet(IQueueExtent.class);
        FurnitureEditProcessor processor = null;
        if (queue != null) {
            processor = new FurnitureEditProcessor(world, queue);
            extent = extent.addProcessor(processor);
        }
        event.setExtent(new GardensWorldEditExtent(extent, world, processor));
    }

    static boolean isStoreBacked(BlockType type) {
        int id = type.getInternalId();
        return id < storeBacked.length && storeBacked[id];
    }

    static BaseBlock toProxy(BlockState state, FurnitureStore store) {
        var data = storeData(store).putString("state", state.getAsString());
        var nbt = LinCompoundTag.builder().put(PROXY_KEY, data.build()).build();
        return PROXY.toBaseBlock(LazyReference.computed(nbt));
    }

    /** History entry for an overwritten store, at the block's position like FAWE's own block entities. */
    static LinCompoundTag toRestoreTag(int x, int y, int z, FurnitureStore store) {
        return LinCompoundTag.builder()
                .putInt("x", x).putInt("y", y).putInt("z", z)
                .put(PROXY_KEY, storeData(store).build())
                .build();
    }

    private static LinCompoundTag.Builder storeData(FurnitureStore store) {
        var data = LinCompoundTag.builder().putString("item", store.getItemIdentifier());
        if (store.getFacing() != null) {
            data.putString("facing", store.getFacing());
        }
        if (store.getMultiplier() != 0) {
            data.putInt("multiplier", store.getMultiplier());
        }
        return data;
    }

    /** The store in a proxy's or history entry's NBT, as it was saved; null if the NBT isn't ours. */
    @Nullable
    static FurnitureStore storeOf(@Nullable LinCompoundTag nbt) {
        var data = nbt == null ? null : nbt.findTag(PROXY_KEY, LinTagType.compoundTag());
        var item = data == null ? null : data.findTag("item", LinTagType.stringTag());
        if (item == null) {
            return null;
        }
        LinStringTag facing = data.findTag("facing", LinTagType.stringTag());
        LinIntTag multiplier = data.findTag("multiplier", LinTagType.intTag());
        return new FurnitureStore(item.value(), multiplier == null ? 0 : multiplier.valueAsInt(), facing == null ? null : facing.value());
    }

    record Placed(BlockState state, FurnitureStore store) {}

    /** The block and store a placed proxy stands for, turned as far as the proxy was; null if it isn't one. */
    @Nullable
    static Placed fromProxy(BlockState placed, @Nullable LinCompoundTag nbt) {
        if (nbt == null || placed.getBlockType() != BlockTypes.BARREL) {
            return null;
        }
        var store = storeOf(nbt);
        var data = nbt.findTag(PROXY_KEY, LinTagType.compoundTag());
        var stateString = data == null ? null : data.findTag("state", LinTagType.stringTag());
        if (store == null || stateString == null) {
            return null;
        }
        BlockState state;
        try {
            state = BlockState.get(stateString.value());
        } catch (InputParseException | IllegalArgumentException e) {
            GardensFurniture.plugin.getLogger().warning("WorldEdit: unknown furniture block state " + stateString.value());
            return null;
        }
        var turn = turnOf(placed);
        state = BlockTransformExtent.transform(state, turn);
        String facing = store.getFacing() == null ? null : turnFace(store.getFacing(), turn);
        return new Placed(state, new FurnitureStore(store.getItemIdentifier(), store.getMultiplier(), facing));
    }

    private static AffineTransform turnOf(BlockState proxy) {
        Direction facing = proxy.getState(PropertyKey.FACING);
        for (var turn : TURNS) {
            if (BlockTransformExtent.transform(PROXY, turn).getState(PropertyKey.FACING) == facing) {
                return turn;
            }
        }
        return TURNS.getFirst(); // turned onto its side (x/z rotation): keep the original orientation
    }

    // stores keep facings as BlockFace names, which match WorldEdit's Direction names
    private static String turnFace(String face, AffineTransform turn) {
        Direction direction;
        try {
            direction = Direction.valueOf(face);
        } catch (IllegalArgumentException e) {
            return face;
        }
        Direction turned = BlockTransformExtent.transform(PROXY.with(PropertyKey.FACING, direction), turn).getState(PropertyKey.FACING);
        return turned.name();
    }

    /** A store to write at a position, or to remove when {@code store} is null. */
    record StoreChange(int x, int y, int z, @Nullable FurnitureStore store) {}

    /**
     * Which blocks of a chunk an edit set, copied out for when the chunk's stores weren't cached while the
     * edit ran - they're checked against it on the main thread instead.
     */
    record SetBlocks(int chunkX, int chunkZ, int minLayer, char[][] layers) {

        static SetBlocks copyOf(int chunkX, int chunkZ, IBlocks set) {
            int min = set.getMinSectionPosition();
            var layers = new char[set.getMaxSectionPosition() - min + 1][];
            for (int layer = min; layer < min + layers.length; layer++) {
                if (set.hasSection(layer)) {
                    var blocks = set.loadIfPresent(layer);
                    layers[layer - min] = blocks == null ? null : blocks.clone();
                }
            }
            return new SetBlocks(chunkX, chunkZ, min, layers);
        }

        boolean isSet(int x, int y, int z) {
            int layer = (y >> 4) - minLayer;
            if (layer < 0 || layer >= layers.length || layers[layer] == null) {
                return false;
            }
            return layers[layer][index(x, y, z)] != BlockTypesCache.ReservedIDs.__RESERVED__;
        }
    }

    static boolean isSet(IBlocks set, int x, int y, int z) {
        int layer = y >> 4;
        if (layer < set.getMinSectionPosition() || layer > set.getMaxSectionPosition() || !set.hasSection(layer)) {
            return false;
        }
        var blocks = set.loadIfPresent(layer);
        return blocks != null && blocks[index(x, y, z)] != BlockTypesCache.ReservedIDs.__RESERVED__;
    }

    private static int index(int x, int y, int z) {
        return (y & 15) << 8 | (z & 15) << 4 | (x & 15);
    }

    /** Applies an edit's store changes on the main thread, in the order edits hand them over. */
    static void apply(String worldName, List<StoreChange> changes, @Nullable SetBlocks uncached) {
        Bukkit.getScheduler().runTask(GardensFurniture.plugin, () -> {
            World world = Bukkit.getWorld(worldName);
            if (world == null) return;
            var handler = FurnitureStoreHandler.INSTANCE;
            var placed = new ArrayList<Block>();
            // an edit is not player activity: keep it out of Prism
            FurnitureStoreHandler.suppressLifecycle.set(true);
            try {
                if (uncached != null) {
                    // before this chunk's own placements, which are set blocks too
                    for (var block : handler.storedBlocks(world.getChunkAt(uncached.chunkX(), uncached.chunkZ()))) {
                        if (uncached.isSet(block.getX(), block.getY(), block.getZ())) {
                            handler.removeFurnitureStore(block);
                        }
                    }
                }
                for (var change : changes) {
                    var block = world.getBlockAt(change.x(), change.y(), change.z());
                    if (change.store() == null) {
                        if (handler.getFurnitureStore(block) != null) {
                            handler.removeFurnitureStore(block);
                        }
                    } else {
                        handler.addFurnitureStore(block, change.store(), false);
                        placed.add(block);
                    }
                }
            } finally {
                FurnitureStoreHandler.suppressLifecycle.set(false);
            }
            if (!placed.isEmpty()) {
                // FAWE may already have sent these blocks undisguised; resend them once it has placed them
                Bukkit.getScheduler().runTaskLater(GardensFurniture.plugin, () -> resend(world, placed), 2);
            }
        });
    }

    private static void resend(World world, List<Block> blocks) {
        var states = blocks.stream()
                .filter(block -> CustomBlockManager.isStoreBacked(block.getType()))
                .map(Block::getState)
                .toList();
        if (states.isEmpty()) return;
        for (var player : world.getPlayers()) {
            int range = (player.getViewDistance() + 1) << 4;
            var location = player.getLocation();
            var near = states.stream()
                    .filter(state -> Math.abs(state.getX() - location.getBlockX()) <= range
                            && Math.abs(state.getZ() - location.getBlockZ()) <= range)
                    .toList();
            if (!near.isEmpty()) {
                player.sendBlockChanges(near);
            }
        }
    }
}
