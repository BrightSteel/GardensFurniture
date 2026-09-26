package com.gardensmc.gardensfurniture.mapletap;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTEntity;
import org.bukkit.Bukkit;
import com.gardensmc.gardensfurniture.events.FurniturePlaceEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class MapleTapHandler implements Listener {

    public static final MapleTapHandler INSTANCE = new MapleTapHandler();
    public static final String TAP_NBT = "GardensFurnitureMapleTap";

    private static final String MAPLE_ID = "maple_tap";
    private static final int FINAL_STAGE = 5;
    private static final String INITIAL_TAG = "initial";
    // timings in seconds from nbt generation
    private static final Map<String, Timing> stageTimings = Map.of(
            "stage_2", new Timing(2, 4),
            "stage_3", new Timing(20, 30),
            "stage_4", new Timing(60, 70),
            "stage_5", new Timing(120, 130)
    );

    private MapleTapHandler() {
    }

    // tap displays in loaded chunks, kept up to date by entity load/unload and placement instead of
    // scanning (and reading the NBT of) every item display in every world each second
    private final Set<ItemDisplay> taps = new HashSet<>();

    public void tick() {
        taps.removeIf(display -> !display.isValid());
        for (var display : List.copyOf(taps)) {
            var tap = getTap(display);
            if (tap != null) {
                tickTap(tap);
            }
        }
    }

    // pick up taps in chunks that were already loaded when the plugin enabled
    public void trackLoadedTaps() {
        Bukkit.getWorlds().forEach(world -> world.getEntitiesByClass(ItemDisplay.class).forEach(this::track));
    }

    private void track(Entity entity) {
        if (entity instanceof ItemDisplay display && display.getItemStack() != null) {
            var customItem = GardensFurniture.itemRegistry.getCustomItem(display.getItemStack());
            if (customItem != null && customItem.getIdentifier().startsWith(MAPLE_ID)) {
                taps.add(display);
            }
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        e.getEntities().forEach(this::track);
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent e) {
        e.getEntities().forEach(taps::remove);
    }

    @EventHandler
    public void onTapPlaced(FurniturePlaceEvent e) {
        track(e.getFurniture().itemDisplay());
    }

    private void tickTap(Tap tap) {
        var nbt = new NBTEntity(tap.display).getPersistentDataContainer();
        if (!nbt.hasTag(TAP_NBT)) {
            generateTapNBT(tap, nbt);
        } else {
            var tapNBT = Objects.requireNonNull(nbt.getCompound(TAP_NBT));
            var initialAge = tapNBT.getLong(INITIAL_TAG);
            var currentTime = tap.display.getWorld().getGameTime();
            var targetStage = tap.stage;
            for (var stage : stageTimings.keySet()) {
                var targetTime = tapNBT.getInteger(stage) + initialAge;
                if (currentTime >= targetTime && stage.compareTo(targetStage) > 0) {
                    targetStage = stage;
                }
            }
            if (!targetStage.equals(tap.stage)) {
                // replace stage
                var nextStageId = MAPLE_ID + "_" + targetStage.substring("stage_".length());
                var itemStack = GardensFurniture.itemRegistry.buildItem(nextStageId);
                if (itemStack == null) {
                    Bukkit.getLogger().log(Level.SEVERE, "Failed to get next maple tap stage: " + nextStageId);
                } else {
                    tap.display.setItemStack(itemStack);
                }
            }
        }
    }

    private void generateTapNBT(Tap tap, NBTCompound nbt) {
        var tapNBT = nbt.getOrCreateCompound(TAP_NBT);
        var worldAge = tap.display.getWorld().getGameTime();
        // generate stage timings
        tapNBT.setLong(INITIAL_TAG, worldAge);
        for (var timingEntry : stageTimings.entrySet()) {
            var timing = timingEntry.getValue();
            var time = ThreadLocalRandom.current().nextInt(timing.min, timing.max) * 20;
            tapNBT.setInteger(timingEntry.getKey(), time);
        }
    }

    @Nullable
    private Tap getTap(ItemDisplay itemDisplay) {
        if (itemDisplay.getItemStack() == null) return null;

        var customItem = GardensFurniture.itemRegistry.getCustomItem(itemDisplay.getItemStack());
        if (customItem == null) return null;

        var identifier = customItem.getIdentifier();
        if (!identifier.startsWith(MAPLE_ID)) return null;

        try {
            int stage = Integer.parseInt(identifier.substring(MAPLE_ID.length() + 1));
            if (stage >= FINAL_STAGE) return null; // no need to tick
            return new Tap(itemDisplay, customItem, "stage_" + stage);
        } catch (NumberFormatException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Incorrectly formatted maple tap with id: " + identifier);
            return null;
        }
    }

    private record Tap(ItemDisplay display, CustomItem customItem, String stage) {
    }

    // inclusive min, exclusive max
    private record Timing(int min, int max) {
    }
}
