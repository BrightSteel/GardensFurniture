package com.gardensmc.gardensfurniture.breaking;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.listener.BlockBreakingListener;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

public class SoundBreakingBlock {

    private final Block block;
    private final String sound;
    private final BukkitTask task;

    public SoundBreakingBlock(Block block, String sound) {
        this.block = block;
        this.sound = sound;
        this.task = Bukkit.getScheduler().runTaskTimer(
                GardensFurniture.plugin, this::tick, 3, 4
        );
    }

    private void tick() {
        block.getWorld().playSound(block.getLocation(), sound, SoundCategory.BLOCKS, 0.25f, 0.5f);
    }

    public void close() {
        task.cancel();
        BlockBreakingListener.soundBreakingBlocks.remove(block);
    }
}
