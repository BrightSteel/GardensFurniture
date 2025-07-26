package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.*;
import com.gardensmc.gardensfurniture.custom.item.*;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.Map.entry;

/**
 * Written by referencing HMCLeaves https://github.com/HibiscusMC/HMCLeaves/blob/master/src/main/java/io/github/fisher2911/hmcleaves/packet/BlockListener.java
 * And by using code from SC2Listener that had to be retired because I couldn't figure out how to initialize the ViaVersion block palette correctly.
 * Realistic Seasons and other biome-adders basically caused clients to crash with that
 */
public class BlockPacketListener {

    private static final LoadingCache<String, Integer> recentPacketsSent = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .build(new CacheLoader<>() {
                @Override
                public Integer load(String key) {
                    return 0;
                }
            });

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
//                if (event.getPacketType() == PacketType.Play.Server.DECLARE_RECIPES) {
//                    var wrapper = new WrapperPlayServerDeclareRecipes(event);
//                    var recipes = wrapper.getRecipes();
//                    for (int i = 0; i < recipes.length; i++) {
//                        var recipe = recipes[i];
//                        if (recipe.getIdentifier().startsWith("customcrafting:cc_display.gardens")) {
//                            var data = recipe.getData();
//                            try {
//                                if (data instanceof ShapedRecipeData recipeData) {
//                                    Field category = recipeData.getClass().getDeclaredField("category");
//                                    category.setAccessible(true);
//                                    category.set(recipeData, CraftingCategory.BUILDING);
//                                } else if (data instanceof ShapelessRecipeData recipeData) {
//                                    Field category = recipeData.getClass().getDeclaredField("category");
//                                    category.setAccessible(true);
//                                    category.set(recipeData, CraftingCategory.BUILDING);
//
//                                }
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                    }
//                }
                if (!(event.getPlayer() instanceof Player player)) return;
                if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
                    // editing this packet directly takes too long and causes ping spikes to the client.
                    // instead we can allow the packet to proceed unedited, edit a copy of it asynchronously, then send it to the client
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
            }
        });
    }

    private void sendChunkDataPacket(Player player, WrapperPlayServerChunkData packet) {
        var key = makeRecentPacketKey(player, packet);
        int packetsSentCount = recentPacketsSent.getUnchecked(key);
        if (packetsSentCount == 2) {
            return;
        }
        recentPacketsSent.put(key, packetsSentCount + 1);
        handleChunkChange(packet, player);
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, packet);
    }

    private String makeRecentPacketKey(Player player, WrapperPlayServerChunkData packet) {
        var column = packet.getColumn();
        return player.getUniqueId() + "-" + column.getX() + "-" + column.getZ();
    }

    private void handleChunkChange(WrapperPlayServerChunkData packet, Player player) {
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

                        if (leavesTypes.contains(blockState.getType())) {
                            CustomItem customItem = getCustomItemFromPos(player.getWorld(), realX, realY, realZ);
                            if (customItem instanceof CustomPlaceableItem customBlockItem && customBlockItem.getCustomBlock() instanceof CustomLeavesBlock customLeavesBlock) {
                                baseChunk.set(x, y, z, getBlockStateFromLeaves(customLeavesBlock, blockState));
                            }
                            else {
                                baseChunk.set(x, y, z, getNormalLeaveState(blockState));
                            }
                        }
                        else if (blockState.getType() == StateTypes.NOTE_BLOCK) {
                            CustomItem customItem = getCustomItemFromPos(player.getWorld(), realX, realY, realZ);
                            if (customItem instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomNoteBlock) {
                                var customNoteBlock = getCustomNoteBlock(placeable, world, realX, realY, realZ);
                                baseChunk.set(x, y, z, getBlockStateFromNoteBlock(customNoteBlock));
                            } else {
                                baseChunk.set(x, y, z, getNormalNoteBlockState(blockState));
                            }
                        }
                        else if (blockState.getType() == StateTypes.CAVE_VINES_PLANT) {
                            CustomItem customItem = getCustomItemFromPos(player.getWorld(), realX, realY, realZ);
                            if (customItem instanceof CustomCaveVineItem customCaveVineItem) {
                                CustomCaveVineBlock customCaveVineBlock = customCaveVineItem.getPlant();
                                baseChunk.set(x, y, z, getBlockStateFromVine(customCaveVineBlock, blockState.isBerries()));
                            }

                        }
                        else if (blockState.getType() == StateTypes.CAVE_VINES) {
                            CustomItem customItem = getCustomItemFromPos(world, realX, realY, realZ);
                            if (customItem instanceof CustomCaveVineItem customCaveVineItem) {
                                CustomCaveVineBlock customCaveVineBlock;
                                if (blockState.getAge() == 25) {
                                    customCaveVineBlock = customCaveVineItem.getVine_25();
                                }
                                else {
                                    customCaveVineBlock = customCaveVineItem.getVine();
                                }
                                baseChunk.set(x, y, z, getBlockStateFromVine(customCaveVineBlock, blockState.isBerries()));
                            }
                            else {
                                baseChunk.set(x, y, z, getNormalVineState(blockState.isBerries()));
                            }
                        }
                        else if (blockState.getType() == StateTypes.TRIPWIRE) {
                            CustomItem customItem = getCustomItemFromPos(world, realX, realY, realZ);
                            if (customItem instanceof CustomPlaceableItem customBlockItem && customBlockItem.getCustomBlock() instanceof CustomTripwireBlock) {
                                var customTripwireBlock = getCustomTripwireBlock(customBlockItem, world, realX, realY, realZ);
                                baseChunk.set(x, y, z, getBlockStateFromTripwire(customTripwireBlock));
                            } else {
                                baseChunk.set(x, y, z, getNormalTripwire(blockState));
                            }
                        } else if (waxedTypes.containsKey(blockState.getType())) {
                            CustomItem customItem = getCustomItemFromPos(player.getWorld(), realX, realY, realZ);
                            if (customItem == null) {
                                baseChunk.set(x, y, z, getUnwaxedState(blockState));
                            }
                        } else if (saplingTypes.contains(blockState.getType())) {
                            CustomItem customItem = getCustomItemFromPos(player.getWorld(), realX, realY, realZ);
                            if (customItem instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomSaplingBlock) {
                                baseChunk.set(x, y, z, getBlockStateFromSapling(blockState.getType()));
                            } else {
                                baseChunk.set(x, y, z, getNormalSapling(blockState));
                            }
                        }
                    }
                }
            }
        }
    }

    private CustomTripwireBlock getCustomTripwireBlock(CustomPlaceableItem customPlaceableItem, World world, int x, int y, int z) {
        if (customPlaceableItem instanceof CustomMultiplierItem customMultiplierItem) {
            var furnitureStore = getStoreFromPos(world, x, y, z);
            int multiplier = furnitureStore.getMultiplier();
            return (CustomTripwireBlock) customMultiplierItem.getCustomBlock(multiplier);
        } else if (customPlaceableItem instanceof CustomFacingItem customFacingItem) {
            var furnitureStore = getStoreFromPos(world, x, y, z);
            String facing = furnitureStore.getFacing();
            return (CustomTripwireBlock) customFacingItem.getCustomBlock(facing);
        }
        return (CustomTripwireBlock) customPlaceableItem.getCustomBlock();
    }

    private CustomNoteBlock getCustomNoteBlock(CustomPlaceableItem customPlaceableItem, World world, int x, int y, int z) {
        if (customPlaceableItem instanceof CustomDirectionalItem customDirectionalItem) {
            var store = getStoreFromPos(world, x, y, z);
            return (CustomNoteBlock) customDirectionalItem.getCustomBlock(BlockFace.valueOf(store.getFacing()));
        }
        return (CustomNoteBlock) customPlaceableItem.getCustomBlock();
    }

    @Nullable
    private CustomItem getCustomItemFromPos(World world, int x, int y, int z) {
        return GardensFurniture.itemRegistry.getItem(getIdentifierFromPos(world, x, y, z));
    }

    private FurnitureStore getStoreFromPos(World world, int x, int y, int z) {
        return FurnitureStoreHandler.INSTANCE.getFurnitureStore(world.getChunkAt(new Location(world, x, y, z)), x, y, z);
    }

    private String getIdentifierFromPos(World world, int x, int y, int z) {
        var store = getStoreFromPos(world, x, y, z);
        if (store == null) return null;
        return store.getItemIdentifier();
    }

    private boolean handleBlockChange(WrapperPlayServerBlockChange packet, Player player) {
        Vector3i pos = packet.getBlockPosition();
        WrappedBlockState blockState = packet.getBlockState();
        if (leavesTypes.contains(blockState.getType())) {
            CustomItem customItem = getCustomItemFromPos(player.getWorld(), pos.getX(), pos.getY(), pos.getZ());
            if (customItem instanceof CustomPlaceableItem customBlockItem && customBlockItem.getCustomBlock() instanceof CustomLeavesBlock customLeavesBlock) {
                WrappedBlockState blockState1 = getBlockStateFromLeaves(customLeavesBlock, blockState);
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                        player,
                        new WrapperPlayServerBlockChange(pos, blockState1.getGlobalId())
                );
                return true;
            }
            else {
                WrappedBlockState blockState1 = getNormalLeaveState(blockState);
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                        player,
                        new WrapperPlayServerBlockChange(pos, blockState1.getGlobalId())
                );
                return true;
            }
        }
        if (blockState.getType() == StateTypes.NOTE_BLOCK) {
            CustomItem customItem = getCustomItemFromPos(player.getWorld(), pos.getX(), pos.getY(), pos.getZ());
            if (customItem instanceof CustomPlaceableItem customBlockItem && customBlockItem.getCustomBlock() instanceof CustomNoteBlock) {
                var customNoteBlock = getCustomNoteBlock(customBlockItem, player.getWorld(), pos.getX(), pos.getY(), pos.getZ());
                WrappedBlockState blockState1 = getBlockStateFromNoteBlock(customNoteBlock);
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                        player,
                        new WrapperPlayServerBlockChange(pos, blockState1.getGlobalId())
                );
                return true;
            }
            else {
                WrappedBlockState blockState1 = getNormalNoteBlockState(blockState);
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                        player,
                        new WrapperPlayServerBlockChange(pos, blockState1.getGlobalId())
                );
                return true;
            }
        }
        else if (waxedTypes.containsKey(blockState.getType())) {
            CustomItem customItem = getCustomItemFromPos(player.getWorld(), pos.getX(), pos.getY(), pos.getZ());
            if (customItem == null) {
                WrappedBlockState blockState1 = getUnwaxedState(blockState);
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                        player,
                        new WrapperPlayServerBlockChange(pos, blockState1.getGlobalId())
                );
                return true;
            }
        }
        else if (blockState.getType() == StateTypes.TRIPWIRE) {
            CustomItem customItem = getCustomItemFromPos(player.getWorld(), pos.getX(), pos.getY(), pos.getZ());
            if (customItem instanceof CustomPlaceableItem customBlockItem && customBlockItem.getCustomBlock() instanceof CustomTripwireBlock) {
                var customTripwireBlock = getCustomTripwireBlock(customBlockItem, player.getWorld(), pos.getX(), pos.getY(), pos.getZ());
                WrappedBlockState blockState1 = getBlockStateFromTripwire(customTripwireBlock);
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                        player,
                        new WrapperPlayServerBlockChange(pos, blockState1.getGlobalId())
                );
                return true;
            }
            else {
                WrappedBlockState blockState1 = getNormalTripwire(blockState);
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                        player,
                        new WrapperPlayServerBlockChange(pos, blockState1.getGlobalId())
                );
                return true;
            }
        }
        else if (blockState.getType() == StateTypes.CAVE_VINES_PLANT) {
            CustomItem customItem = getCustomItemFromPos(player.getWorld(), pos.getX(), pos.getY(), pos.getZ());
            if (customItem instanceof CustomCaveVineItem customCaveVineItem) {
                CustomCaveVineBlock plantBlock = customCaveVineItem.getPlant();
                WrappedBlockState blockState1 = getBlockStateFromVine(plantBlock, blockState.isBerries());
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                        player,
                        new WrapperPlayServerBlockChange(pos, blockState1.getGlobalId())
                );
                return true;
            }
        }
        else if (blockState.getType() == StateTypes.CAVE_VINES) {
            CustomItem customItem = getCustomItemFromPos(player.getWorld(), pos.getX(), pos.getY(), pos.getZ());
            if (customItem instanceof CustomCaveVineItem customCaveVineItem) {
                CustomCaveVineBlock vineBlock;
                if (blockState.getAge() == 25) {
                    vineBlock = customCaveVineItem.getVine_25();
                }
                else {
                    vineBlock = customCaveVineItem.getVine();
                }
                WrappedBlockState blockState1 = getBlockStateFromVine(vineBlock, blockState.isBerries());
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                        player,
                        new WrapperPlayServerBlockChange(pos, blockState1.getGlobalId())
                );
                return true;
            }
            else {
                WrappedBlockState blockState1 = getNormalVineState(blockState.isBerries());
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                        player,
                        new WrapperPlayServerBlockChange(pos, blockState1.getGlobalId())
                );
                return true;
            }
        } else if (saplingTypes.contains(blockState.getType())) {
            CustomItem customItem = getCustomItemFromPos(player.getWorld(), pos.getX(), pos.getY(), pos.getZ());
            if (customItem instanceof CustomPlaceableItem customBlockItem && customBlockItem.getCustomBlock() instanceof CustomSaplingBlock) {
                WrappedBlockState blockState1 = getBlockStateFromSapling(blockState.getType());
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                        player,
                        new WrapperPlayServerBlockChange(pos, blockState1.getGlobalId())
                );
                return true;
            }
            else {
                WrappedBlockState blockState1 = getNormalSapling(blockState);
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                        player,
                        new WrapperPlayServerBlockChange(pos, blockState1.getGlobalId())
                );
                return true;
            }
        }
        return false;
    }

    private void handleMultiBlockChange(WrapperPlayServerMultiBlockChange packet, Player player) {
        var blocks = packet.getBlocks();
        for (var block : blocks) {
            WrappedBlockState blockState1 = block.getBlockState(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());
            if (leavesTypes.contains(blockState1.getType())) {
                CustomItem customItem = getCustomItemFromPos(player.getWorld(), block.getX(), block.getY(), block.getZ());
                if (customItem instanceof CustomPlaceableItem customBlockItem && customBlockItem.getCustomBlock() instanceof CustomLeavesBlock customLeavesBlock) {
                    WrappedBlockState blockState = getBlockStateFromLeaves(customLeavesBlock, blockState1);
                    block.setBlockState(blockState);
                }
                else {
                    block.setBlockState(getNormalLeaveState(blockState1));
                }
            }
            if (blockState1.getType() == StateTypes.NOTE_BLOCK) {
                CustomItem customItem = getCustomItemFromPos(player.getWorld(), block.getX(), block.getY(), block.getZ());
                if (customItem instanceof CustomPlaceableItem customBlockItem && customBlockItem.getCustomBlock() instanceof CustomNoteBlock) {
                    var customNoteBlock = getCustomNoteBlock(customBlockItem, player.getWorld(), block.getX(), block.getY(), block.getZ());
                    WrappedBlockState blockState = getBlockStateFromNoteBlock(customNoteBlock);
                    block.setBlockState(blockState);
                }
                else {
                    block.setBlockState(getNormalNoteBlockState(blockState1));
                }
            }
            if (waxedTypes.containsKey(blockState1.getType())) {
                CustomItem customItem = getCustomItemFromPos(player.getWorld(), block.getX(), block.getY(), block.getZ());
                if (customItem == null) {
                    WrappedBlockState blockState = getUnwaxedState(blockState1);
                    block.setBlockState(blockState);
                }
            }
            else if (blockState1.getType() == StateTypes.TRIPWIRE) {
                CustomItem customItem = getCustomItemFromPos(player.getWorld(), block.getX(), block.getY(), block.getZ());
                if (customItem instanceof CustomPlaceableItem customBlockItem && customBlockItem.getCustomBlock() instanceof CustomTripwireBlock) {
                    var customTripwireBlock = getCustomTripwireBlock(customBlockItem, player.getWorld(), block.getX(), block.getY(), block.getZ());
                    WrappedBlockState blockState = getBlockStateFromTripwire(customTripwireBlock);
                    block.setBlockState(blockState);
                }
                else {
                    block.setBlockState(getNormalTripwire(blockState1));
                }
            }
            else if (blockState1.getType() == StateTypes.CAVE_VINES_PLANT) {
                CustomItem customItem = getCustomItemFromPos(player.getWorld(), block.getX(), block.getY(), block.getZ());
                if (customItem instanceof CustomCaveVineItem customCaveVineItem) {
                    WrappedBlockState blockState = getBlockStateFromVine(customCaveVineItem.getPlant(), blockState1.isBerries());
                    block.setBlockState(blockState);
                }
            }
            else if (blockState1.getType() == StateTypes.CAVE_VINES) {
                CustomItem customItem = getCustomItemFromPos(player.getWorld(), block.getX(), block.getY(), block.getZ());
                if (customItem instanceof CustomCaveVineItem customCaveVineItem) {
                    CustomCaveVineBlock customCaveVineBlock;
                    if (blockState1.getAge() == 25) {
                        customCaveVineBlock = customCaveVineItem.getVine_25();
                    }
                    else {
                        customCaveVineBlock = customCaveVineItem.getVine();
                    }
                    WrappedBlockState blockState = getBlockStateFromVine(customCaveVineBlock, blockState1.isBerries());
                    block.setBlockState(blockState);
                }
                else {
                    block.setBlockState(getNormalVineState(blockState1.isBerries()));
                }
            } else if (saplingTypes.contains(blockState1.getType())) {
                CustomItem customItem = getCustomItemFromPos(player.getWorld(), block.getX(), block.getY(), block.getZ());
                if (customItem instanceof CustomPlaceableItem customBlockItem && customBlockItem.getCustomBlock() instanceof CustomSaplingBlock) {
                    WrappedBlockState blockState = getBlockStateFromSapling(blockState1.getType());
                    block.setBlockState(blockState);
                }
                else {
                    block.setBlockState(getNormalSapling(blockState1));
                }
            }
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
