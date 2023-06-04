package com.gardensmc.gardensfurniture.listener;

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
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
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

import java.util.List;

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
                        if (blockState.getType() == StateTypes.SPRUCE_LEAVES) {
                            int realX = coordinateX + x;
                            int realZ = coordinateZ + z;
                            int realY = y + (16 * i) - Math.abs(worldMinHeight);
                            Chunk chunk = world.getChunkAt(new Location(world, realX, realY, realZ));
                            String identifier = chunk.getPersistentDataContainer().get(new NamespacedKey("gardens", realX + "." + realY + "." + realZ), PersistentDataType.STRING);
                            if (identifier != null) {
                                CustomItem customItem = CustomRegister.INSTANCE.itemRegistry.getItem(identifier);
                                if (customItem != null && customItem.getCustomBlock() instanceof CustomLeavesBlock customLeavesBlock) {
                                    baseChunk.set(x, y, z, getBlockStateFromLeaves(customLeavesBlock));
                                }
                            }
                            else {
                                baseChunk.set(x, y, z, getNormalLeaveState());
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean handleBlockChange(WrapperPlayServerBlockChange packet, Player player) {
        Vector3i pos = packet.getBlockPosition();
        WrappedBlockState blockState = packet.getBlockState();
        if (blockState.getType() == StateTypes.SPRUCE_LEAVES) {
            Chunk chunk = player.getWorld().getChunkAt(new Location(player.getWorld(), pos.getX(), pos.getY(), pos.getZ()));
            String itemid = chunk.getPersistentDataContainer().get(new NamespacedKey("gardens", pos.getX() + "." + pos.getY() + "." + pos.getZ()), PersistentDataType.STRING);
            if (itemid != null) {
                CustomItem customItem = CustomRegister.INSTANCE.itemRegistry.getItem(itemid);
                if (customItem != null && customItem.getCustomBlock() instanceof CustomLeavesBlock customLeavesBlock) {
                    WrappedBlockState blockState1 = getBlockStateFromLeaves(customLeavesBlock);
                    PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                            player,
                            new WrapperPlayServerBlockChange(pos, blockState1.getGlobalId())
                    );
                    return true;
                }
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
        return false;
    }

    private void handleMultiBlockChange(WrapperPlayServerMultiBlockChange packet, Player player) {
        var blocks = packet.getBlocks();
        for (var block : blocks) {
            if (block.getBlockState(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()).getType() == StateTypes.SPRUCE_LEAVES) {
                Chunk chunk = player.getWorld().getChunkAt(new Location(player.getWorld(), block.getX(), block.getY(), block.getZ()));
                String identifier = chunk.getPersistentDataContainer().get(new NamespacedKey("gardens", block.getX() + "." + block.getY() + "." + block.getZ()), PersistentDataType.STRING);
                if (identifier != null) {
                    CustomItem customItem = CustomRegister.INSTANCE.itemRegistry.getItem(identifier);
                    if (customItem != null && customItem.getCustomBlock() instanceof CustomLeavesBlock customLeavesBlock) {
                        WrappedBlockState blockState = getBlockStateFromLeaves(customLeavesBlock);
                        block.setBlockState(blockState);
                    }
                } else {
                    block.setBlockState(getNormalLeaveState());
                }
            }
        }

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
}
