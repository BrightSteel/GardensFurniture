package com.gardensmc.gardensfurniture.loader;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;

import java.util.Objects;

public record ChunkReference(int x, int z, String world) {

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ChunkReference chunkReference) {
            return chunkReference.x == this.x && chunkReference.z == this.z && chunkReference.world.equals(this.world);
        }
        return false;
    }

    public Chunk toChunk() {
        return Objects.requireNonNull(Bukkit.getWorld(world)).getChunkAt(x, z);
    }

    public static ChunkReference create(Chunk chunk) {
        return new ChunkReference(chunk.getX(), chunk.getZ(), chunk.getWorld().getName());
    }
}
