package com.gardensmc.gardensfurniture.listener.sounds;

import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.block.CustomWaxedBlock;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import net.minecraft.world.level.block.SoundType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Openable;
import org.bukkit.craftbukkit.CraftSoundGroup;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class VanillaBlockSoundListener implements Listener {

    public static float VANILLA_PLACE_VOLUME = 1.0f;
    public static float VANILLA_PLACE_PITCH = 0.8f;
    public static float VANILLA_BREAK_VOLUME = 1.0f;
    public static float VANILLA_BREAK_PITCH = 0.8f;
    public static float VANILLA_STEP_VOLUME = 0.15f;
    public static float VANILLA_STEP_PITCH = 1.0f;
    public static float VANILLA_FALL_VOLUME = 0.5f;
    public static float VANILLA_FALL_PITCH = 0.75f;

    public static float DOOR_VOLUME = 1.0f;
    public static float DOOR_PITCH = 0.9f;

    private static final Map<SoundType, String> SOUND_TYPE_OVERRIDES_TO_NAME = Map.of(
            SoundType.WOOD, "wood",
            SoundType.COPPER, "copper",
            SoundType.STONE, "stone",
            SoundType.GRASS, "grass"
    );

    private String getDoorSoundToPlay(Block block, DoorType doorType, String suffix) {
        var customItem = CustomBlockManager.getCustomItemFromBlock(block);
        if (customItem instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomWaxedBlock) {
            var materialTraits = placeable.getCustomBlock().getMaterialTraits();
            // just ignoring actual value for now since we only care about wood sounds...
            if (materialTraits != null) {
                return "block.wooden_" + doorType.toString().toLowerCase() + "." + suffix;
            }
        }
        return "vanilla.copper_" + doorType.toString().toLowerCase() + "." + suffix;
    }

    @Nullable
    public static String getSoundToPlay(Block block, String suffix) {
        var customItem = CustomBlockManager.getCustomItemFromBlock(block);
        if (customItem instanceof CustomPlaceableItem placeable) {
            var materialTraits = placeable.getCustomBlock().getMaterialTraits();
            if (materialTraits != null) {
                var soundGroup = materialTraits.createBlockData().getSoundGroup();
                var soundOverride = getSoundOverride(soundGroup, suffix);
                if (soundOverride != null) {
                    return soundOverride;
                }
                var traitsKey = switch (suffix) {
                    case "break" -> soundGroup.getBreakSound();
                    case "place" -> soundGroup.getPlaceSound();
                    case "fall" -> soundGroup.getFallSound();
                    case "hit" -> soundGroup.getHitSound();
                    case "step" -> soundGroup.getStepSound();
                    default -> throw new IllegalArgumentException("Unknown sound suffix: " + suffix);
                };
                return traitsKey.getKey().getKey().toLowerCase();
            }
        }
        return getSoundOverride(block.getBlockData().getSoundGroup(), suffix);
    }

    public static String getSoundOverride(SoundGroup soundGroup, String suffix) {
        var soundTypeName = SOUND_TYPE_OVERRIDES_TO_NAME.get(((CraftSoundGroup) soundGroup).getHandle());
        if (soundTypeName != null) {
            return "vanilla." + soundTypeName + "." + suffix;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent e) {
        var block = e.getBlock();
        var sound = getSoundToPlay(block, "break");
        if (sound != null) {
            block.getWorld().playSound(block.getLocation(), sound, SoundCategory.BLOCKS, VANILLA_BREAK_VOLUME, VANILLA_BREAK_PITCH);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent e) {
        var block = e.getBlockPlaced();
        var sound = getSoundToPlay(block, "place");
        if (sound != null) {
            block.getWorld().playSound(block.getLocation(), sound, SoundCategory.BLOCKS, VANILLA_PLACE_VOLUME, VANILLA_PLACE_PITCH);
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
        var block = currentBlock.getType().isAir() || currentBlock.getType() == Material.LIGHT ? currentBlock.getRelative(BlockFace.DOWN) : currentBlock;
        if (gameEvent == GameEvent.STEP) {
            var sound = getSoundToPlay(block, "step");
            if (sound != null) {
                block.getWorld().playSound(block.getLocation(), sound, SoundCategory.BLOCKS, VANILLA_STEP_VOLUME, VANILLA_STEP_PITCH);
            }
        } else {
            var sound = getSoundToPlay(block, "fall");
            if (sound != null) {
                block.getWorld().playSound(block.getLocation(), sound, SoundCategory.BLOCKS, VANILLA_FALL_VOLUME, VANILLA_FALL_PITCH);
            }
        }
    }

    @EventHandler
    public void onDoorTrapdoorToggle(GenericGameEvent e) {
        if (!e.getLocation().isWorldLoaded()) return;
        if (e.getEvent() == GameEvent.BLOCK_OPEN || e.getEvent() == GameEvent.BLOCK_CLOSE) {
            var block = e.getLocation().getBlock();
            if (block.getBlockData() instanceof Openable) {
                var doorType = DoorType.fromType(block.getType());
                if (doorType != null) {
                    var sound = getDoorSoundToPlay(block, doorType, e.getEvent() == GameEvent.BLOCK_OPEN ? "open" : "close");
                    block.getWorld().playSound(block.getLocation(), sound, SoundCategory.BLOCKS, DOOR_VOLUME, DOOR_PITCH);
                }
            }
        }
    }

    private enum DoorType {
        DOOR, TRAPDOOR;

        public static DoorType fromType(Material type) {
            var typeStr = type.toString().toLowerCase();
            if (typeStr.contains("copper_door")) {
                return DOOR;
            } else if (typeStr.contains("copper_trapdoor")) {
                return TRAPDOOR;
            }
            return null;
        }
    }


}
