package com.gardensmc.gardensfurniture.breaking;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.listener.BlockBreakingListener;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class SoundBreakingBlock {

    private static final long TICK_PERIOD = 4L;
    private static final long TICK_DELAY = 3L;

    private final Block block;
    private final String sound;
    @Getter
    private final UUID playerUUID;
    // safety cap: the sound stops itself even if the abort/break event that would
    // normally close it never fires (player looks away mid-dig, desync, etc.)
    private final long maxTicks;
    private final BukkitTask task;

    private long ticksElapsed = TICK_DELAY;

    public SoundBreakingBlock(Block block, String sound, UUID playerUUID, long maxTicks) {
        this.block = block;
        this.sound = sound;
        this.playerUUID = playerUUID;
        this.maxTicks = maxTicks;
        this.task = Bukkit.getScheduler().runTaskTimer(
                GardensFurniture.plugin, this::tick, TICK_DELAY, TICK_PERIOD
        );
    }

    private void tick() {
        block.getWorld().playSound(block.getLocation(), sound, SoundCategory.BLOCKS, 0.25f, 0.5f);
        if (ticksElapsed >= maxTicks) {
            close();
            return;
        }
        ticksElapsed += TICK_PERIOD;
    }

    public void close() {
        task.cancel();
        BlockBreakingListener.soundBreakingBlocks.remove(block);
    }
}
