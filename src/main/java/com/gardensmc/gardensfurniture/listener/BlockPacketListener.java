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
import joptsimple.internal.Strings;
import org.bukkit.*;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Map.entry;

/**
 * Written by referencing HMCLeaves https://github.com/HibiscusMC/HMCLeaves/blob/master/src/main/java/io/github/fisher2911/hmcleaves/packet/BlockListener.java
 * And by using code from SC2Listener that had to be retired because I couldn't figure out how to initialize the ViaVersion block palette correctly.
 * Realistic Seasons and other biome-adders basically caused clients to crash with that
 */
public class BlockPacketListener {

    // invisible crystal types
    private final Set<StateType> crystalTypes = Set.of(
            StateTypes.SMALL_AMETHYST_BUD,
            StateTypes.MEDIUM_AMETHYST_BUD,
            StateTypes.LARGE_AMETHYST_BUD,
            StateTypes.AMETHYST_CLUSTER
    );

    private final Set<StateType> leavesTypes = Set.of(
            StateTypes.SPRUCE_LEAVES,
            StateTypes.CHERRY_LEAVES,
            StateTypes.BIRCH_LEAVES
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
                    handleChunkChange(new WrapperPlayServerChunkData(event), player);
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
                                baseChunk.set(x, y, z, getBlockStateFromLeaves(customLeavesBlock, blockState.getType()));
                            }
                            else {
                                baseChunk.set(x, y, z, getNormalLeaveState(blockState.getType()));
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
                            else if (customItem instanceof CustomCaveVineDirectional customCaveVineDirectional) {
                                String id = getIdentifierFromPos(world, realX, realY, realZ);
                                CustomCaveVineBlock customCaveVineBlock = getHiddenCaveBlock(id, customCaveVineDirectional);
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
                        } else if (blockState.getType() == StateTypes.BUDDING_AMETHYST) {
                            CustomItem customItem = getCustomItemFromPos(world, realX, realY, realZ);
                            if (customItem instanceof CustomPlaceableItem placeableItem && placeableItem.getCustomBlock() instanceof CustomBuddingBlock budding) {
                                baseChunk.set(x, y, z, getBlockStateFromNote(budding));
                            }
                        } else if (blockState.getType() == StateTypes.OAK_SAPLING) {

                        } else if (waxedTypes.containsKey(blockState.getType())) {
                            CustomItem customItem = getCustomItemFromPos(player.getWorld(), realX, realY, realZ);
                            if (customItem == null) {
                                baseChunk.set(x, y, z, getUnwaxedState(blockState));
                            }
                        }
//                        } else if (crystalTypes.contains(blockState.getType())) {
//                            var crystalBlock = getCustomItemFromPos(world, realX, realY, realZ);
//                            if (crystalBlock instanceof CustomPlaceableItem placeableItem && placeableItem.getCustomBlock() instanceof CustomCrystalBlock crystalBlock1) {
//                                baseChunk.set(x, y, z, getBlockStateFromCrystal(blockState));
//                            }
//                        }
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

    private CustomCaveVineBlock getHiddenCaveBlock(String identifier, CustomCaveVineDirectional customCaveVineDirectional) {
        String hidden = identifier.split("\\.")[1];
        CustomCaveVineBlock customCaveVineBlock;
        switch (hidden) {
            default -> customCaveVineBlock = customCaveVineDirectional.getVine_south();
            case "EAST" -> customCaveVineBlock = customCaveVineDirectional.getVine_west();
            case "SOUTH" -> customCaveVineBlock = customCaveVineDirectional.getVine_north();
            case "WEST" -> customCaveVineBlock = customCaveVineDirectional.getVine_east();
        }
        return customCaveVineBlock;
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
                WrappedBlockState blockState1 = getBlockStateFromLeaves(customLeavesBlock, blockState.getType());
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                        player,
                        new WrapperPlayServerBlockChange(pos, blockState1.getGlobalId())
                );
                return true;
            }
            else {
                WrappedBlockState blockState1 = getNormalLeaveState(blockState.getType());
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
        else if (blockState.getType() == StateTypes.BUDDING_AMETHYST) {
            CustomItem customItem = getCustomItemFromPos(player.getWorld(), pos.getX(), pos.getY(), pos.getZ());
            if (customItem instanceof CustomPlaceableItem placeableItem && placeableItem.getCustomBlock() instanceof CustomBuddingBlock customBuddingBlock) {
                var bState = getBlockStateFromNote(customBuddingBlock);
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                        player,
                        new WrapperPlayServerBlockChange(pos, bState.getGlobalId())
                );
                return true;
            }
        }
//        else if (crystalTypes.contains(blockState.getType())) {
//            var crystal = getCustomItemFromPos(player.getWorld(), pos.getX(), pos.getY(), pos.getZ());
//            if (crystal instanceof CustomPlaceableItem placeableItem && placeableItem.getCustomBlock() instanceof CustomCrystalBlock crystalBlock) {
//                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
//                        player,
//                        new WrapperPlayServerBlockChange(pos, getBlockStateFromCrystal(blockState).getGlobalId())
//                );
//                return true;
//            }
//        }
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
            else if (customItem instanceof CustomCaveVineDirectional customCaveVineDirectional) {
                String id = getIdentifierFromPos(player.getWorld(), pos.getX(), pos.getY(), pos.getZ());
                CustomCaveVineBlock customCaveVineBlock = getHiddenCaveBlock(id, customCaveVineDirectional);
                WrappedBlockState blockState1 = getBlockStateFromVine(customCaveVineBlock, blockState.isBerries());
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
                    WrappedBlockState blockState = getBlockStateFromLeaves(customLeavesBlock, blockState1.getType());
                    block.setBlockState(blockState);
                }
                else {
                    block.setBlockState(getNormalLeaveState(blockState1.getType()));
                }
            }
            if (waxedTypes.containsKey(blockState1.getType())) {
                CustomItem customItem = getCustomItemFromPos(player.getWorld(), block.getX(), block.getY(), block.getZ());
                if (customItem == null) {
                    WrappedBlockState blockState = getUnwaxedState(blockState1);
                    block.setBlockState(blockState);
                }
            }
            else if (blockState1.getType() == StateTypes.BUDDING_AMETHYST) {
                CustomItem customItem = getCustomItemFromPos(player.getWorld(), block.getX(), block.getY(), block.getZ());
                if (customItem instanceof CustomPlaceableItem placeableItem && placeableItem.getCustomBlock() instanceof CustomBuddingBlock buddingBlock) {
                    block.setBlockState(getBlockStateFromNote(buddingBlock));
                }
            }
//            else if (crystalTypes.contains(blockState1.getType())) {
//                var crystal = getCustomItemFromPos(player.getWorld(), block.getX(), block.getY(), block.getZ());
//                if (crystal instanceof CustomPlaceableItem placeableItem && placeableItem.getCustomBlock() instanceof CustomCrystalBlock crystalBlock) {
//                    block.setBlockState(getBlockStateFromCrystal(blockState1));
//                }
//            }
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
                else if (customItem instanceof CustomCaveVineDirectional customCaveVineDirectional) {
                    String id = getIdentifierFromPos(player.getWorld(), block.getX(), block.getY(), block.getZ());
                    CustomCaveVineBlock customCaveVineBlock = getHiddenCaveBlock(id, customCaveVineDirectional);
                    WrappedBlockState blockState = getBlockStateFromVine(customCaveVineBlock, blockState1.isBerries());
                    block.setBlockState(blockState);
                }
                else {
                    block.setBlockState(getNormalVineState(blockState1.isBerries()));
                }
            }
        }

    }

    private WrappedBlockState getBlockStateFromCrystal(WrappedBlockState blockstate) {
//        WrappedBlockState blockState = StateTypes.getByName(customCrystalBlock.getType().getCrystalMaterial().toString()).createBlockState();
//        blockState.setWaterlogged(true);
        WrappedBlockState chorusState = StateTypes.CHORUS_PLANT.createBlockState();
        chorusState.setDown(false);
        switch (blockstate.getFacing()) {
            case WEST -> chorusState.setEast(East.TRUE);
            case EAST -> chorusState.setWest(West.TRUE);
            case SOUTH -> chorusState.setNorth(North.TRUE);
            case NORTH -> chorusState.setSouth(South.TRUE);
            case DOWN -> chorusState.setUp(true);
        }
//        blockState.setDown(true);
//        blockState.setUp(true);
//        blockState.setNorth(North.TRUE);
//        blockState.setEast(East.TRUE);
//        blockState.setWest(West.TRUE);
//        blockState.setSouth(South.TRUE);
        return chorusState;
    }

    private WrappedBlockState getBlockStateFromVine(CustomCaveVineBlock customCaveVineBlock, boolean berries) {
        WrappedBlockState blockState = StateTypes.CAVE_VINES.createBlockState();
        blockState.setAge(customCaveVineBlock.getAge());
        if (customCaveVineBlock.allowGlow()) {
            blockState.setBerries(berries); // only send berry state if custom vine allows glow
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

    private WrappedBlockState getBlockStateFromLeaves(CustomLeavesBlock customLeavesBlock, StateType leafType) {
        WrappedBlockState blockState = leafType.createBlockState();
        blockState.setDistance(customLeavesBlock.getDistance());
        blockState.setPersistent(customLeavesBlock.isPersistent());
        return blockState;
    }

    private WrappedBlockState getBlockStateFromNote(CustomNoteBlock customNoteBlock) {
        var blockState = StateTypes.NOTE_BLOCK.createBlockState();
        blockState.setNote(customNoteBlock.getNote().getId());
        blockState.setInstrument(Instrument.valueOf(customNoteBlock.getInstrument().toString()));
        return blockState;
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

    private WrappedBlockState getNormalTripwire(WrappedBlockState blockstate) {
        // strip off un-needed states so we can use them in conjunction with the directions
        blockstate.setPowered(false);
        blockstate.setDisarmed(false);
        return blockstate;
    }

    private WrappedBlockState getNormalAmethyst(WrappedBlockState blockState) {
        blockState.setWaterlogged(false);
        return blockState;
    }

    private WrappedBlockState getNormalLeaveState(StateType stateType) {
        WrappedBlockState blockState = stateType.createBlockState();
        blockState.setDistance(1);
        blockState.setPersistent(false);
        return blockState;
    }

    private WrappedBlockState getNormalVineState(boolean berries) {
        WrappedBlockState blockState = StateTypes.CAVE_VINES.createBlockState();
        blockState.setAge(0);
        blockState.setBerries(berries);
        return blockState;
    }
}
