package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.breaking.CustomBreakingBlock;
import com.gardensmc.gardensfurniture.custom.block.*;
import com.gardensmc.gardensfurniture.custom.item.*;
import com.gardensmc.gardensfurniture.store.FurnitureCache;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.enums.East;
import com.github.retrooper.packetevents.protocol.world.states.enums.Instrument;
import com.github.retrooper.packetevents.protocol.world.states.enums.North;
import com.github.retrooper.packetevents.protocol.world.states.enums.South;
import com.github.retrooper.packetevents.protocol.world.states.enums.West;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

/**
 * Written by referencing HMCLeaves https://github.com/HibiscusMC/HMCLeaves/blob/master/src/main/java/io/github/fisher2911/hmcleaves/packet/BlockListener.java
 * And by using code from SC2Listener that had to be retired because I couldn't figure out how to initialize the ViaVersion block palette correctly.
 * Realistic Seasons and other biome-adders basically caused clients to crash with that
 */
public class BlockPacketListener {

    private final Set<StateType> leavesTypes = Set.of(
            StateTypes.SPRUCE_LEAVES,
            StateTypes.CHERRY_LEAVES,
            StateTypes.OAK_LEAVES,
            StateTypes.BIRCH_LEAVES
    );

    private final Set<StateType> saplingTypes = Set.of(
            StateTypes.OAK_SAPLING,
            StateTypes.DARK_OAK_SAPLING
    );

    private final Map<StateType, StateType> waxedTypes = Map.ofEntries(
            entry(StateTypes.WAXED_CUT_COPPER_STAIRS, StateTypes.CUT_COPPER_STAIRS),
            entry(StateTypes.WAXED_CUT_COPPER_SLAB, StateTypes.CUT_COPPER_SLAB),
            entry(StateTypes.WAXED_COPPER_TRAPDOOR, StateTypes.COPPER_TRAPDOOR),
            entry(StateTypes.WAXED_COPPER_DOOR, StateTypes.COPPER_DOOR),

            entry(StateTypes.WAXED_EXPOSED_CUT_COPPER_STAIRS, StateTypes.EXPOSED_CUT_COPPER_STAIRS),
            entry(StateTypes.WAXED_EXPOSED_CUT_COPPER_SLAB, StateTypes.EXPOSED_CUT_COPPER_SLAB),
            entry(StateTypes.WAXED_EXPOSED_COPPER_TRAPDOOR, StateTypes.EXPOSED_COPPER_TRAPDOOR),
            entry(StateTypes.WAXED_EXPOSED_COPPER_DOOR, StateTypes.EXPOSED_COPPER_DOOR),

            entry(StateTypes.WAXED_WEATHERED_CUT_COPPER_STAIRS, StateTypes.WEATHERED_CUT_COPPER_STAIRS),
            entry(StateTypes.WAXED_WEATHERED_CUT_COPPER_SLAB, StateTypes.WEATHERED_CUT_COPPER_SLAB),
            entry(StateTypes.WAXED_WEATHERED_COPPER_TRAPDOOR, StateTypes.WEATHERED_COPPER_TRAPDOOR),
            entry(StateTypes.WAXED_WEATHERED_COPPER_DOOR, StateTypes.WEATHERED_COPPER_DOOR),

            entry(StateTypes.WAXED_OXIDIZED_CUT_COPPER_STAIRS, StateTypes.OXIDIZED_CUT_COPPER_STAIRS),
            entry(StateTypes.WAXED_OXIDIZED_CUT_COPPER_SLAB, StateTypes.OXIDIZED_CUT_COPPER_SLAB),
            entry(StateTypes.WAXED_OXIDIZED_COPPER_TRAPDOOR, StateTypes.OXIDIZED_COPPER_TRAPDOOR),
            entry(StateTypes.WAXED_OXIDIZED_COPPER_DOOR, StateTypes.OXIDIZED_COPPER_DOOR)
    );

    public void register() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (!(event.getPlayer() instanceof Player player)) return;
                if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
                    // editing this packet directly takes too long and causes ping spikes to the client.
                    // instead we can allow the packet to proceed unedited, edit a copy of it asynchronously, then send it to the client
                    event.setCancelled(true);
                    var packet = new WrapperPlayServerChunkData(event);
                    var copy = new WrapperPlayServerChunkData(packet.getColumn(), packet.getLightData(), packet.isIgnoreOldData());
                    Bukkit.getScheduler().runTaskAsynchronously(GardensFurniture.plugin, () -> sendChunkDataPacket(player, copy));
                }
                else if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
                    if (handleBlockChange(new WrapperPlayServerBlockChange(event), player)) {
                        event.setCancelled(true);
                    }
                }
                else if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
                    handleMultiBlockChange(new WrapperPlayServerMultiBlockChange(event), player);
                }
                else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
                    handleSetSlot(new WrapperPlayServerSetSlot(event));
                }
                else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
                    handleWindowItems(new WrapperPlayServerWindowItems(event));
                }
                else if (event.getPacketType() == PacketType.Play.Server.SET_PLAYER_INVENTORY) {
                    handleSetPlayerInventory(new WrapperPlayServerSetPlayerInventory(event));
                }
            }
        });
    }

    // Hotbar occupies menu slots 36-44 of the player inventory (window 0).
    private static final int HOTBAR_FIRST_SLOT = 36;
    private static final int HOTBAR_LAST_SLOT = 44;

    // The client predicts block breaking from the tool in its hand, so BlockBreakingListener patches the
    // client's held items with a rule that makes the custom-breaking block types near-unbreakable. Any
    // real inventory update the server sends (join sync, durability change, item move) would revert that
    // patch and let the next dig mispredict/insta-break; re-apply the patch to every hotbar slot the server
    // sends. Slots are patched by index only (no inventory lookup) so this is safe off the main thread.
    private void handleSetSlot(WrapperPlayServerSetSlot packet) {
        int slot = packet.getSlot();
        boolean hotbar;
        if (packet.getWindowId() == 0) {
            hotbar = slot >= HOTBAR_FIRST_SLOT && slot <= HOTBAR_LAST_SLOT; // menu slots 36-44
        } else if (packet.getWindowId() == -2) {
            hotbar = slot >= 0 && slot <= 8; // direct player-inventory update: raw inventory index
        } else {
            return;
        }
        if (!hotbar) return;
        var patched = suppressBreakingFor(packet.getItem());
        if (patched != null) {
            packet.setItem(patched);
        }
    }

    // Newer per-slot player-inventory update (1.21.2+). Its slot is the raw inventory index, so the
    // hotbar is 0-8 here (not the 36-44 menu range that SET_SLOT/WINDOW_ITEMS use).
    private void handleSetPlayerInventory(WrapperPlayServerSetPlayerInventory packet) {
        if (packet.getSlot() < 0 || packet.getSlot() > 8) return;
        var patched = suppressBreakingFor(packet.getStack());
        if (patched != null) {
            packet.setStack(patched);
        }
    }

    private void handleWindowItems(WrapperPlayServerWindowItems packet) {
        if (packet.getWindowId() != 0) return;
        var items = packet.getItems();
        boolean modified = false;
        for (int slot = HOTBAR_FIRST_SLOT; slot <= HOTBAR_LAST_SLOT && slot < items.size(); slot++) {
            var patched = suppressBreakingFor(items.get(slot));
            if (patched != null) {
                items.set(slot, patched);
                modified = true;
            }
        }
        if (modified) {
            packet.setItems(items);
        }
    }

    // Returns a patched packet item, or null when there's nothing to change (empty slot).
    private com.github.retrooper.packetevents.protocol.item.ItemStack suppressBreakingFor(
            com.github.retrooper.packetevents.protocol.item.ItemStack packetItem) {
        if (packetItem == null || packetItem.isEmpty()) return null;
        var bukkit = SpigotConversionUtil.toBukkitItemStack(packetItem);
        if (bukkit == null || bukkit.getType().isAir()) return null;
        return SpigotConversionUtil.fromBukkitItemStack(BlockBreakingListener.withBreakSuppressionTool(bukkit));
    }

    private void sendChunkDataPacket(Player player, WrapperPlayServerChunkData packet) {
        try {
            handleChunkChange(packet, player);
        } catch (Exception e) {
            // still send the (partly remapped) chunk - never leave the client with a hole
            GardensFurniture.plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to remap chunk packet", e);
        }
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, packet);
    }

    // returns true if modified packet otherwise false
    private boolean handleChunkChange(WrapperPlayServerChunkData packet, Player player) {
        boolean modified = false;
        Column column = packet.getColumn();
        BaseChunk[] chunks = column.getChunks();
        World world = player.getWorld();
        var coordinateX = column.getX() * 16;
        var coordinateZ = column.getZ() * 16;
        int worldMinHeight = world.getMinHeight();
        for (int i = 0; i < chunks.length; i++) {
            BaseChunk baseChunk = chunks[i];
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        WrappedBlockState blockState = baseChunk.get(x, y, z);
                        int realX = coordinateX + x;
                        int realZ = coordinateZ + z;
                        int realY = y + (16 * i) - Math.abs(worldMinHeight);

                        WrappedBlockState remapped;
                        try {
                            remapped = remap(world, realX, realY, realZ, blockState);
                        } catch (RuntimeException e) {
                            // one bad block must not stop the rest of the chunk from being remapped
                            logRemapFailure(e);
                            continue;
                        }
                        if (remapped != null) {
                            baseChunk.set(x, y, z, remapped);
                            modified = true;
                        }
                    }
                }
            }
        }
        return modified;
    }

    private CustomTripwireBlock getCustomTripwireBlock(CustomPlaceableItem customPlaceableItem, World world, int x, int y, int z) {
        var furnitureStore = getStoreFromPos(world, x, y, z);
        if (customPlaceableItem instanceof CustomMultiplierItem customMultiplierItem && furnitureStore != null && furnitureStore.getMultiplier() > 0) {
            return (CustomTripwireBlock) customMultiplierItem.getCustomBlock(furnitureStore.getMultiplier());
        } else if (customPlaceableItem instanceof CustomFacingItem customFacingItem && furnitureStore != null && furnitureStore.getFacing() != null) {
            return (CustomTripwireBlock) customFacingItem.getCustomBlock(furnitureStore.getFacing());
        }
        return (CustomTripwireBlock) customPlaceableItem.getCustomBlock();
    }

    private CustomNoteBlock getCustomNoteBlock(CustomPlaceableItem customPlaceableItem, World world, int x, int y, int z) {
        if (customPlaceableItem instanceof CustomDirectionalItem customDirectionalItem) {
            var store = getStoreFromPos(world, x, y, z);
            if (store != null && store.getFacing() != null) {
                return (CustomNoteBlock) customDirectionalItem.getCustomBlock(BlockFace.valueOf(store.getFacing()));
            }
        }
        return (CustomNoteBlock) customPlaceableItem.getCustomBlock();
    }

    @Nullable
    private CustomItem getCustomItemFromPos(World world, int x, int y, int z) {
        return GardensFurniture.itemRegistry.getItem(getIdentifierFromPos(world, x, y, z));
    }

    private FurnitureStore getStoreFromPos(World world, int x, int y, int z) {
        // runs on packet threads: build the key from coordinates instead of touching the chunk
        return FurnitureCache.INSTANCE.getFurnitureStore(
                new FurnitureCache.ChunkKey(world.getName(), x >> 4, z >> 4),
                new FurnitureCache.BlockKey(x, y, z)
        );
    }

    private String getIdentifierFromPos(World world, int x, int y, int z) {
        var store = getStoreFromPos(world, x, y, z);
        if (store == null) return null;
        return store.getItemIdentifier();
    }

    private boolean handleBlockChange(WrapperPlayServerBlockChange packet, Player player) {
        Vector3i pos = packet.getBlockPosition();
        WrappedBlockState blockState = packet.getBlockState();
        // While a barrier is being broken it's shown to the digger as the invisible break block so the
        // crack animation renders. The server re-sends the real barrier (e.g. to reject the dig on an
        // unbreakable block); rewrite those back to the break block so the illusion isn't clobbered.
        if (blockState.getType() == StateTypes.BARRIER) {
            if (BlockBreakingListener.isBreakingBarrierAt(player.getUniqueId(), pos.getX(), pos.getY(), pos.getZ())) {
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                        player,
                        new WrapperPlayServerBlockChange(pos, CustomBreakingBlock.getInvisibleBreakBlockId())
                );
                return true;
            }
            return false;
        }
        var remapped = remap(player.getWorld(), pos.getX(), pos.getY(), pos.getZ(), blockState);
        if (remapped == null) {
            return false;
        }
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, new WrapperPlayServerBlockChange(pos, remapped.getGlobalId()));
        return true;
    }

    private void handleMultiBlockChange(WrapperPlayServerMultiBlockChange packet, Player player) {
        var blocks = packet.getBlocks();
        for (var block : blocks) {
            var blockState = block.getBlockState(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());
            var remapped = remap(player.getWorld(), block.getX(), block.getY(), block.getZ(), blockState);
            if (remapped != null) {
                block.setBlockState(remapped);
            }
        }
    }

    /**
     * The state a client should see for a block the server is sending, or null to send it unchanged.
     * Custom blocks are shown as their custom state; the vanilla block types we use as disguises are
     * normalised so a plain vanilla block never looks like a custom one. Shared by chunk, block-change
     * and multi-block-change packets, and called on packet threads.
     */
    @Nullable
    private WrappedBlockState remap(World world, int x, int y, int z, WrappedBlockState state) {
        var type = state.getType();
        if (leavesTypes.contains(type)) {
            return getCustomItemFromPos(world, x, y, z) instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomLeavesBlock leaves
                    ? getBlockStateFromLeaves(leaves, state)
                    : getNormalLeaveState(state);
        }
        if (type == StateTypes.NOTE_BLOCK) {
            return getCustomItemFromPos(world, x, y, z) instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomNoteBlock
                    ? getBlockStateFromNoteBlock(getCustomNoteBlock(placeable, world, x, y, z))
                    : getNormalNoteBlockState(state);
        }
        if (type == StateTypes.TRIPWIRE) {
            return getCustomItemFromPos(world, x, y, z) instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomTripwireBlock
                    ? getBlockStateFromTripwire(getCustomTripwireBlock(placeable, world, x, y, z))
                    : getNormalTripwire(state);
        }
        if (type == StateTypes.CAVE_VINES_PLANT) {
            return getCustomItemFromPos(world, x, y, z) instanceof CustomCaveVineItem vineItem
                    ? getBlockStateFromVine(vineItem.getPlant(), state.isBerries())
                    : null;
        }
        if (type == StateTypes.CAVE_VINES) {
            if (getCustomItemFromPos(world, x, y, z) instanceof CustomCaveVineItem vineItem) {
                var vine = state.getAge() == 25 ? vineItem.getVine_25() : vineItem.getVine();
                return getBlockStateFromVine(vine, state.isBerries());
            }
            return getNormalVineState(state.isBerries());
        }
        if (waxedTypes.containsKey(type)) {
            // waxed copper is only ever custom; a vanilla waxed block is shown unwaxed
            return getCustomItemFromPos(world, x, y, z) == null ? getUnwaxedState(state) : null;
        }
        if (saplingTypes.contains(type)) {
            return getCustomItemFromPos(world, x, y, z) instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomSaplingBlock
                    ? getBlockStateFromSapling(type)
                    : getNormalSapling(state);
        }
        return null;
    }

    private volatile long lastRemapFailureLog;

    private void logRemapFailure(RuntimeException e) {
        long now = System.currentTimeMillis();
        if (now - lastRemapFailureLog > 60_000) { // at most once a minute
            lastRemapFailureLog = now;
            GardensFurniture.plugin.getLogger().log(java.util.logging.Level.WARNING, "Failed to remap a block in a chunk packet", e);
        }
    }

    private WrappedBlockState getBlockStateFromVine(CustomCaveVineBlock customCaveVineBlock, boolean berries) {
        WrappedBlockState blockState = StateTypes.CAVE_VINES.createBlockState();
        blockState.setAge(customCaveVineBlock.getAge());
        if (customCaveVineBlock.isGrowBerries()) {
            blockState.setBerries(berries);
        }
        return blockState;
    }

    private WrappedBlockState getUnwaxedState(WrappedBlockState blockState) {
       var state = waxedTypes.get(blockState.getType()).createBlockState();
       var typeName = state.getType().toString().toUpperCase();
       // copy over data
        if (typeName.endsWith("_DOOR")) {
            state.setFacing(blockState.getFacing());
            state.setHalf(blockState.getHalf());
            state.setHinge(blockState.getHinge());
            state.setOpen(blockState.isOpen());
            state.setPowered(blockState.isPowered());
        } else if (typeName.endsWith("_TRAPDOOR")) {
            state.setFacing(blockState.getFacing());
            state.setHalf(blockState.getHalf());
            state.setOpen(blockState.isOpen());
            state.setPowered(blockState.isPowered());
            state.setWaterlogged(blockState.isWaterlogged());
        } else if (typeName.endsWith("_STAIRS")) {
            state.setFacing(blockState.getFacing());
            state.setHalf(blockState.getHalf());
            state.setShape(blockState.getShape());
            state.setWaterlogged(blockState.isWaterlogged());
        } else if (typeName.endsWith("_SLAB")) {
            state.setTypeData(blockState.getTypeData());
            state.setWaterlogged(blockState.isWaterlogged());
        }
        return state;
    }

    private WrappedBlockState getBlockStateFromLeaves(CustomLeavesBlock customLeavesBlock, WrappedBlockState blockState) {
        blockState.setDistance(customLeavesBlock.getDistance());
        blockState.setPersistent(customLeavesBlock.isPersistent());
        return blockState;
    }

    private WrappedBlockState getBlockStateFromNoteBlock(CustomNoteBlock customNoteBlock) {
        WrappedBlockState blockState = StateTypes.NOTE_BLOCK.createBlockState();
        blockState.setNote(customNoteBlock.getNote().getId());
        blockState.setInstrument(getPacketInstrument(customNoteBlock));
        blockState.setPowered(customNoteBlock.isPowered());
        return blockState;
    }

    private Instrument getPacketInstrument(CustomNoteBlock customNoteBlock) {
        return switch (customNoteBlock.getInstrument()) {
            case PIANO -> Instrument.HARP;
            case BASS_DRUM -> Instrument.BASEDRUM;
            case SNARE_DRUM -> Instrument.SNARE;
            case BASS_GUITAR -> Instrument.BASS;
            case STICKS -> Instrument.HAT;
            default -> Instrument.valueOf(customNoteBlock.getInstrument().toString());
        };
    }

    private WrappedBlockState getBlockStateFromTripwire(CustomTripwireBlock customTripwireBlock) {
        WrappedBlockState blockState = StateTypes.TRIPWIRE.createBlockState();
        blockState.setNorth(North.valueOf(String.valueOf(customTripwireBlock.isNorth()).toUpperCase()));
        blockState.setSouth(South.valueOf(String.valueOf(customTripwireBlock.isSouth()).toUpperCase()));
        blockState.setEast(East.valueOf(String.valueOf(customTripwireBlock.isEast()).toUpperCase()));
        blockState.setWest(West.valueOf(String.valueOf(customTripwireBlock.isWest()).toUpperCase()));
        blockState.setDisarmed(customTripwireBlock.isDisarmed());
        blockState.setPowered(customTripwireBlock.isPowered());
        blockState.setAttached(customTripwireBlock.isAttached());
        return blockState;
    }

    private WrappedBlockState getBlockStateFromSapling(StateType stateType) {
        WrappedBlockState blockState = stateType.createBlockState();
        blockState.setStage(0);
        return blockState;
    }

    private WrappedBlockState getNormalSapling(WrappedBlockState blockState) {
        blockState.setStage(1);
        return blockState;
    }

    private WrappedBlockState getNormalTripwire(WrappedBlockState blockstate) {
        // strip off un-needed states so we can use them in conjunction with the directions
        blockstate.setPowered(false);
        blockstate.setDisarmed(false);
        return blockstate;
    }

    private WrappedBlockState getNormalLeaveState(WrappedBlockState blockState) {
        blockState.setDistance(1);
        blockState.setPersistent(false);
        return blockState;
    }

    private WrappedBlockState getNormalNoteBlockState(WrappedBlockState blockState) {
        // strip off un-needed states - keep powered in case RP adds some special state for it
        blockState.setNote(0);
        blockState.setInstrument(Instrument.HARP);
        return blockState;
    }

    private WrappedBlockState getNormalVineState(boolean berries) {
        WrappedBlockState blockState = StateTypes.CAVE_VINES.createBlockState();
        blockState.setAge(0);
        blockState.setBerries(berries);
        return blockState;
    }
}
