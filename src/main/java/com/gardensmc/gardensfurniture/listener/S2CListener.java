package com.gardensmc.gardensfurniture.listener;

import com.comphenix.packetwrapper.WrapperPlayServerBlockChange;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomItem;
import com.gardensmc.gardensfurniture.custom.CustomLeavesBlock;
import com.gardensmc.gardensfurniture.leaves.LeaveWorld;
import com.gardensmc.gardensfurniture.register.CustomRegister;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.type.types.version.ChunkSectionType1_18;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import net.minecraft.core.Registry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_19_R1.block.data.CraftBlockData;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class S2CListener {

    private ProtocolManager protocolManager;

    public S2CListener(ProtocolManager protocolManager) {
        this.protocolManager = protocolManager;
        listen();
    }

    private void listen() {
        protocolManager.addPacketListener(new PacketAdapter(
                GardensFurniture.plugin,
                ListenerPriority.NORMAL,
                List.of(
                        PacketType.Play.Server.MAP_CHUNK,
                        PacketType.Play.Server.BLOCK_CHANGE,
                        PacketType.Play.Server.MULTI_BLOCK_CHANGE
                        ),
                ListenerOptions.ASYNC) {

//            @Override
//            public void onPacketSending(PacketEvent event) {
//                super.onPacketSending(event);
//                PacketContainer packet = event.getPacket();
//                System.out.println(packet);
//                System.out.println(event.getPacket().getBlockData().getValues());
//            }

            @Override
            public void onPacketSending(PacketEvent event) {
//                super.onPacketSending(event);
                World playerWorld = event.getPlayer().getWorld();
                var chunkSectionType = new ChunkSectionType1_18(Block.o.b(), 0);
//
                if (event.getPacketType() == PacketType.Play.Server.MAP_CHUNK) {

                    ClientboundLevelChunkWithLightPacket p = (ClientboundLevelChunkWithLightPacket) event.getPacket().getHandle();
                    ClientboundLevelChunkPacketData chunkData = p.d();

                    var chunkX = p.b();
                    var chunkZ = p.c();
                    var coordinateX = chunkX * 16;
                    var coordinateZ = chunkZ * 16;

                    int worldMinHeight = playerWorld.getMinHeight();
                    int worldMaxHeight = playerWorld.getMaxHeight();
                    int worldTrueHeight = (Math.abs(worldMinHeight) + worldMaxHeight);
                    int ySectionCount = worldTrueHeight / 16;

                    PacketDataSerializer readBuffer = chunkData.a();
                    byte[] byteArray = readBuffer.array();


                    ByteBuf buf = Unpooled.copiedBuffer(byteArray);

                    List<ChunkSection> sections = new ArrayList<>();
                    for (int i = 0; i <= ySectionCount; i++) {
                        try {
                            sections.add(chunkSectionType.read(buf));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    boolean requiresEdits = false;

                    int i = 0;
                    for (ChunkSection section : sections) {
                        DataPalette blockPalette = section.palette(PaletteType.BLOCKS);

                        if (blockPalette == null) continue;

                        for (int y = 0; y < 16; y++) {
                            for (int x = 0; x < 16; x++) {
                                for (int z = 0; z < 16; z++) {
                                    int blockId = section.getFlatBlock(x, y, z);
                                    IBlockData blockData = Block.o.a(blockId);
                                    if (blockData != null) {
                                        Material material = CraftBlockData.fromData(blockData).getMaterial();
                                        if (material == Material.SPRUCE_LEAVES) {
                                            var position = new BlockPosition(
                                                    coordinateX + x,
                                                    y + (16 * i) - Math.abs(worldMinHeight), // The sections are from the bottom to the top, so the section 0 is at the world's min height!
                                                    coordinateZ + z
                                            );
                                            LeaveWorld leaveWorld = GardensFurniture.getLeaveWorld(playerWorld);
                                            org.bukkit.block.Block block = playerWorld.getBlockAt(new Location(playerWorld, position.getX(), position.getY(), position.getZ()));
                                            String leaveId = leaveWorld.getLeaveId(block);
                                            if (leaveId != null) System.out.println("leaveId: " + leaveId);
                                            if ((position.getX() == -441 && position.getZ() == 80) || (position.getX() == -441 && position.getZ() == 82) || (position.getX() == -440 && position.getZ() == 82)) {
                                                System.out.println("Leave at: " + position.getX() + ", " + position.getY() + ", " + position.getZ());
                                            }

                                            CustomItem customItem = CustomRegister.INSTANCE.itemRegistry.getItem(leaveId);
                                            if (customItem != null && customItem.getCustomBlock() instanceof CustomLeavesBlock customLeavesBlock) {
                                                System.out.println("custom Leave at: " + position.getX() + ", " + position.getY() + ", " + position.getZ());
                                                section.setFlatBlock(x, y, z, Block.o.a(CraftBlockData.newData(Material.SPRUCE_LEAVES, "[distance=" + customLeavesBlock.getDistance() + ",persistent=" + customLeavesBlock.isPersistent() + "]").getState()));

                                            }
                                            else {
                                                section.setFlatBlock(x, y, z, Block.o.a(CraftBlockData.newData(Material.SPRUCE_LEAVES, "[distance=1,persistent=false]").getState()));
                                            }
                                            requiresEdits = true;

                                        }

                                    }
                                }
                            }
                        }
                        i++;

                        if (requiresEdits) {
                            ByteBuf byteBuf = Unpooled.buffer();
                            sections.stream().filter(chunkSection -> {
                                if (chunkSection == null) return false;
                                return true;
                            }).forEach(chunkSection -> {
                                try {
                                    chunkSectionType.write(byteBuf, chunkSection);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            });

                            try {
                                Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
                                unsafeField.setAccessible(true);
                                Unsafe unsafe = (Unsafe) unsafeField.get(null);
                                Field ourField = ClientboundLevelChunkPacketData.class.getDeclaredField("c");
                                long staticFieldOffset = unsafe.objectFieldOffset(ourField);
                                unsafe.putObject(chunkData, staticFieldOffset, byteBuf.array());
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }



                    // Create an info objects

//                    ints.read(0); 	 // packet.a;
//                    ints.read(1); 	 // packet.b;
//                    byteArray.read(1);  // packet.inflatedBuffer;
                }
                else if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
                    WrapperPlayServerBlockChange wrapper = new WrapperPlayServerBlockChange(event.getPacket());

                    if (wrapper.getBlockData().getType() == Material.SPRUCE_LEAVES) {
                        wrapper.setBlockData(WrappedBlockData.fromHandle(CraftBlockData.newData(Material.SPRUCE_LEAVES, "[distance=1,persistent=false]").getState()));
                    }
                    event.setPacket(wrapper.getHandle());
                }
                else if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
                    BlockPosition blockPosition = event.getPacket().getSectionPositions().read(0);

                    short[] shorts = event.getPacket().getShortArrays().read(0);

                    WrappedBlockData[] changedBlocks = event.getPacket().getBlockDataArrays().read(0);

                    if (Arrays.stream(changedBlocks).noneMatch(wrappedBlockData -> wrappedBlockData.getType() == Material.SPRUCE_LEAVES)) {
                        return;
                    }

                    List<WrappedBlockData> list = new ArrayList<>();

                    for (int i = 0; i < changedBlocks.length; i++) {
                        if (changedBlocks[i].getType() == Material.SPRUCE_LEAVES) {
                            list.add(WrappedBlockData.fromHandle(CraftBlockData.newData(Material.SPRUCE_LEAVES, "[distance=1,persistent=false]").getState()));
                        }
                        else {
                            list.add(changedBlocks[i]);
                        }
                    }
                    event.getPacket().getBlockDataArrays().write(0, list.toArray(new WrappedBlockData[0]));
                }

            }
        }

        );
    }


}
