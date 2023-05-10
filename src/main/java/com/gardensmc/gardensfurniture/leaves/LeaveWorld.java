package com.gardensmc.gardensfurniture.leaves;

import com.gardensmc.gardensfurniture.util.LeavesConfig;
import org.bukkit.Chunk;
import org.bukkit.block.Block;

import javax.annotation.Nullable;
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
        HashMap<String, String> chunkData = LeavesConfig.getChunkData(chunk);
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
        HashMap<String, String> chunkData = LeavesConfig.getChunkData(chunk);
        if (chunkData != null) {
            blockMap.put(chunk.getX() + "," + chunk.getZ(), chunkData);
        }
    }
}
