package com.gardensmc.gardensfurniture.listener.sounds;

import org.bukkit.GameEvent;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.GenericGameEvent;

public class VanillaBlockSoundListener implements Listener {

    public static String VANILLA_WOOD_PLACE = "minecraft:vanilla.wood.place";
    public static String VANILLA_WOOD_BREAK = "minecraft:vanilla.wood.break";
    public static String VANILLA_WOOD_STEP = "minecraft:vanilla.wood.step";
    public static String VANILLA_WOOD_FALL = "minecraft:vanilla.wood.fall";

    public static float VANILLA_PLACE_VOLUME = 1.0f;
    public static float VANILLA_PLACE_PITCH = 0.8f;
    public static float VANILLA_BREAK_VOLUME = 1.0f;
    public static float VANILLA_BREAK_PITCH = 0.8f;
    public static float VANILLA_STEP_VOLUME = 0.15f;
    public static float VANILLA_STEP_PITCH = 1.0f;
    public static float VANILLA_FALL_VOLUME = 0.5f;
    public static float VANILLA_FALL_PITCH = 0.75f;

    public static final String WOOD_SOUND_GROUP = Material.NOTE_BLOCK.createBlockData().getSoundGroup().toString();

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        var block = e.getBlock();
        if (block.getBlockData().getSoundGroup().toString().equals(WOOD_SOUND_GROUP)) {
            block.getWorld().playSound(block.getLocation(), VANILLA_WOOD_BREAK, SoundCategory.BLOCKS, VANILLA_BREAK_VOLUME, VANILLA_BREAK_PITCH);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        var block = e.getBlockPlaced();
        if (block.getBlockData().getSoundGroup().toString().equals(WOOD_SOUND_GROUP)) {
            block.getWorld().playSound(block.getLocation(), VANILLA_WOOD_PLACE, SoundCategory.BLOCKS, VANILLA_PLACE_VOLUME, VANILLA_PLACE_PITCH);
        }
    }

    @EventHandler
    public void onBlockStep(GenericGameEvent e) {
        Entity entity = e.getEntity();
        GameEvent gameEvent = e.getEvent();
        if (gameEvent != GameEvent.HIT_GROUND && gameEvent != GameEvent.STEP) return;
        if (entity == null || !e.getLocation().isWorldLoaded()) return;
        if (!(entity instanceof LivingEntity)) return;
        if (entity instanceof Player player && player.isSneaking()) return;

        var currentBlock = entity.getLocation().getBlock();
        var block = currentBlock.getType().isAir() ? currentBlock.getRelative(BlockFace.DOWN) : currentBlock;
        if (block.getBlockData().getSoundGroup().toString().equals(WOOD_SOUND_GROUP)) {
            if (gameEvent == GameEvent.STEP) {
                block.getWorld().playSound(block.getLocation(), VANILLA_WOOD_STEP, SoundCategory.BLOCKS, VANILLA_STEP_VOLUME, VANILLA_STEP_PITCH);
            } else {
                block.getWorld().playSound(block.getLocation(), VANILLA_WOOD_FALL, SoundCategory.BLOCKS, VANILLA_FALL_VOLUME, VANILLA_FALL_PITCH);
            }
        }
    }
}
