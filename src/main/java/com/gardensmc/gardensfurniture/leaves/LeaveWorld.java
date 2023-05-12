package com.gardensmc.gardensfurniture.leaves;

import com.gardensmc.gardensfurniture.database.LeavesData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.PacketPlayOutMap;
import net.minecraft.network.protocol.game.PacketPlayOutMultiBlockChange;
import net.minecraft.network.protocol.game.PacketPlayOutUnloadChunk;
import net.minecraft.world.level.World;
import net.minecraft.world.level.lighting.LightEngine;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_19_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.HashMap;

public class LeaveWorld {

    // chunkX,chunkZ -> { x,y,z -> leaveId }
    private final HashMap<String, HashMap<String, String>> blockMap = new HashMap<>();

    @Nullable
    public String getLeaveId(Block block) {
        if (blockMap.containsKey(block.getChunk().getX() + "," + block.getChunk().getZ())) {
            return blockMap.get(block.getChunk().getX() + "," + block.getChunk().getZ()).get(block.getX() + "," + block.getY() + "," + block.getZ());
        }
        return null;
    }

    /**
     * Should be run async
     * @param chunk
     */
    public void refreshChunk(Chunk chunk) {
        HashMap<String, String> chunkData;
        try {
            chunkData = LeavesData.INSTANCE.getLeavesInChunk(chunk);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        if (chunkData != null) {
            blockMap.put(chunk.getX() + "," + chunk.getZ(), chunkData);
        }
        else {
            blockMap.remove(chunk.getX() + "," + chunk.getZ());
        }
    }

    public void removeChunk(Chunk chunk) {
        blockMap.remove(chunk.getX() + "," + chunk.getZ());
    }

    public void loadChunk(Chunk chunk) {
        HashMap<String, String> chunkData;
        try {
            chunkData = LeavesData.INSTANCE.getLeavesInChunk(chunk);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        if (chunkData != null) {
            blockMap.put(chunk.getX() + "," + chunk.getZ(), chunkData);
            // send packet to update chunks to any players in view distance
            int view = Bukkit.getViewDistance() << 4;
            int diffx, diffz;
            for (Player player : Bukkit.getOnlinePlayers()) {
                diffx = Math.abs(player.getLocation().getBlockX() - chunk.getX() << 4);
                diffz = Math.abs(player.getLocation().getBlockZ() - chunk.getZ() << 4);
                if (diffx <= view && diffz <= view) {
                    LightEngine light = ((CraftWorld) chunk.getWorld()).getHandle().l_();
                    ((CraftPlayer) player).getHandle().b.a(new ClientboundLevelChunkWithLightPacket(((CraftChunk)chunk).getHandle(), light, new BitSet(), new BitSet(), true));
                }
            }

        }
    }
}
