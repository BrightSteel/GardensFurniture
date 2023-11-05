package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.*;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.enums.East;
import com.github.retrooper.packetevents.protocol.world.states.enums.North;
import com.github.retrooper.packetevents.protocol.world.states.enums.South;
import com.github.retrooper.packetevents.protocol.world.states.enums.West;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;

/**
 * Written by referencing HMCLeaves https://github.com/HibiscusMC/HMCLeaves/blob/master/src/main/java/io/github/fisher2911/hmcleaves/packet/BlockListener.java
 * And by using code from SC2Listener that had to be retired because I couldn't figure out how to initialize the ViaVersion block palette correctly.
 * Realistic Seasons and other biome-adders basically caused clients to crash with that
 */
public class BlockPacketListener {

    public void register() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketSend(PacketSendEvent event) {
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

                        if (blockState.getType() == StateTypes.SPRUCE_LEAVES) {
                            CustomItem customItem = getCustomItemFromPos(world, realX, realY, realZ);
                            if (customItem != null && customItem.getCustomBlock() instanceof CustomLeavesBlock customLeavesBlock) {
                                baseChunk.set(x, y, z, getBlockStateFromLeaves(customLeavesBlock));
                            }
                            else {
                                baseChunk.set(x, y, z, getNormalLeaveState());
                            }
                        }
                        else if (blockState.getType() == StateTypes.CAVE_VINES_PLANT) {
                            CustomItem customItem = getCustomItemFromPos(world, realX, realY, realZ);
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
                            if (customItem != null && customItem.getCustomBlock() instanceof CustomTripwireBlock customTripwireBlock) {
                                baseChunk.set(x, y, z, getBlockStateFromTripwire(customTripwireBlock));
                            }
                            else {
                                baseChunk.set(x, y, z, getNormalTripwire(blockState));
                            }
                        }
                    }
                }
            }
        }
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
        String identifier = getIdentifierFromPos(world, x, y, z);
        if (identifier != null) {
            // remove hidden parts
            String[] parts = identifier.split("\\.");
            return GardensFurniture.customRegister.itemRegistry.getItem(parts[0]);
        }
        return null;
    }

    private String getIdentifierFromPos(World world, int x, int y, int z) {
        Chunk chunk = world.getChunkAt(new Location(world, x, y, z));
        return chunk.getPersistentDataContainer().get(new NamespacedKey("gardens", x + "." + y + "." + z), PersistentDataType.STRING);
    }

    private boolean handleBlockChange(WrapperPlayServerBlockChange packet, Player player) {
        Vector3i pos = packet.getBlockPosition();
        WrappedBlockState blockState = packet.getBlockState();
        if (blockState.getType() == StateTypes.SPRUCE_LEAVES) {
            CustomItem customItem = getCustomItemFromPos(player.getWorld(), pos.getX(), pos.getY(), pos.getZ());
            if (customItem != null && customItem.getCustomBlock() instanceof CustomLeavesBlock customLeavesBlock) {
                WrappedBlockState blockState1 = getBlockStateFromLeaves(customLeavesBlock);
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                        player,
                        new WrapperPlayServerBlockChange(pos, blockState1.getGlobalId())
                );
                return true;
            }
            else {
                WrappedBlockState blockState1 = getNormalLeaveState();
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                        player,
                        new WrapperPlayServerBlockChange(pos, blockState1.getGlobalId())
                );
                return true;
            }
        }
        else if (blockState.getType() == StateTypes.TRIPWIRE) {
            CustomItem customItem = getCustomItemFromPos(player.getWorld(), pos.getX(), pos.getY(), pos.getZ());
            if (customItem != null && customItem.getCustomBlock() instanceof CustomTripwireBlock customTripwireBlock) {
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
            if (blockState1.getType() == StateTypes.SPRUCE_LEAVES) {
                CustomItem customItem = getCustomItemFromPos(player.getWorld(), block.getX(), block.getY(), block.getZ());
                if (customItem != null && customItem.getCustomBlock() instanceof CustomLeavesBlock customLeavesBlock) {
                    WrappedBlockState blockState = getBlockStateFromLeaves(customLeavesBlock);
                    block.setBlockState(blockState);
                }
                else {
                    block.setBlockState(getNormalLeaveState());
                }
            }
            else if (blockState1.getType() == StateTypes.TRIPWIRE) {
                CustomItem customItem = getCustomItemFromPos(player.getWorld(), block.getX(), block.getY(), block.getZ());
                if (customItem != null && customItem.getCustomBlock() instanceof CustomTripwireBlock customTripwireBlock) {
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

    private WrappedBlockState getBlockStateFromVine(CustomCaveVineBlock customCaveVineBlock, boolean berries) {
        WrappedBlockState blockState = StateTypes.CAVE_VINES.createBlockState();
        blockState.setAge(customCaveVineBlock.getAge());
        blockState.setBerries(berries);
        return blockState;
    }

    private WrappedBlockState getBlockStateFromLeaves(CustomLeavesBlock customLeavesBlock) {
        WrappedBlockState blockState = StateTypes.SPRUCE_LEAVES.createBlockState();
        blockState.setDistance(customLeavesBlock.getDistance());
        blockState.setPersistent(customLeavesBlock.isPersistent());
        return blockState;
    }

    private WrappedBlockState getBlockStateFromTripwire(CustomTripwireBlock customTripwireBlock) {
        WrappedBlockState blockState = StateTypes.TRIPWIRE.createBlockState();
        blockState.setNorth(North.valueOf(String.valueOf(customTripwireBlock.isNorth()).toUpperCase()));
        blockState.setSouth(South.valueOf(String.valueOf(customTripwireBlock.isSouth()).toUpperCase()));
        blockState.setEast(East.valueOf(String.valueOf(customTripwireBlock.isEast()).toUpperCase()));
        blockState.setWest(West.valueOf(String.valueOf(customTripwireBlock.isEast()).toUpperCase()));
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

    private WrappedBlockState getNormalLeaveState() {
        WrappedBlockState blockState = StateTypes.SPRUCE_LEAVES.createBlockState();
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
