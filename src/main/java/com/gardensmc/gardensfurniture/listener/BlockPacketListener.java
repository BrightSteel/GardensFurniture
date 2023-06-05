package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.custom.CustomCaveVineBlock;
import com.gardensmc.gardensfurniture.custom.CustomCaveVineItem;
import com.gardensmc.gardensfurniture.custom.CustomItem;
import com.gardensmc.gardensfurniture.custom.CustomLeavesBlock;
import com.gardensmc.gardensfurniture.register.CustomRegister;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
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
                            else {
                                baseChunk.set(x, y, z, getNormalVineState(blockState.isBerries()));
                            }
                        }
                    }
                }
            }
        }
    }

    @Nullable
    private CustomItem getCustomItemFromPos(World world, int x, int y, int z) {
        String identifier = getIdentifierFromPos(world, x, y, z);
        if (identifier != null) {
            return CustomRegister.INSTANCE.itemRegistry.getItem(identifier);
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
