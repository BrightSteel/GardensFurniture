package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.GardensFurniture;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A block.interactions entry: right-clicking the furniture while holding {@code held} (a vanilla material
 * or a custom item id) swaps it for one of {@code replaceWith}, picked at random but never the piece that's
 * already there - so a carved pumpkin re-carved with shears always gets a new face. {@code held: AIR} means
 * an empty hand.
 * <pre>
 * interactions:
 *   - held: SHEARS
 *     replace_with: [halloween_pumpkin_face_1, halloween_pumpkin_face_2]
 *     damage_held: 1     # durability taken from the held item (survival only)
 *     consume: 0         # or how many of the held item are used up
 *     sound: block.pumpkin.carve
 *     sound_pitch_variance: 0.2  # pitch picked from 1 +/- this, like flint and steel's 0.8-1.2
 *     face_clicked: true # turn the new piece's front to the side that was clicked
 *     drop: PUMPKIN_SEEDS # a vanilla material or custom item id, dropped from the clicked side
 *     drop_amount: 4
 * </pre>
 */
public record ReplaceInteraction(String held, List<String> replaceWith, int damageHeld, int consume, @Nullable String sound,
                                 double soundPitchVariance, boolean faceClicked, @Nullable String drop, int dropAmount) {

    public static List<ReplaceInteraction> parse(ConfigurationSection blockSection, String identifier) {
        List<ReplaceInteraction> interactions = new ArrayList<>();
        for (Map<?, ?> map : blockSection.getMapList("interactions")) {
            Object held = map.get("held");
            Object replace = map.get("replace_with");
            List<String> replaceWith = new ArrayList<>();
            if (replace instanceof List<?> list) {
                list.forEach(id -> replaceWith.add(String.valueOf(id)));
            } else if (replace != null) {
                replaceWith.add(String.valueOf(replace));
            }
            if (held == null || replaceWith.isEmpty()) {
                GardensFurniture.plugin.getLogger().warning(identifier + ": an interaction needs both held and replace_with, skipping it");
                continue;
            }
            interactions.add(new ReplaceInteraction(String.valueOf(held), replaceWith,
                    intOf(map.get("damage_held")), intOf(map.get("consume")),
                    map.get("sound") == null ? null : String.valueOf(map.get("sound")),
                    map.get("sound_pitch_variance") instanceof Number variance ? variance.doubleValue() : 0,
                    Boolean.TRUE.equals(map.get("face_clicked")),
                    map.get("drop") == null ? null : String.valueOf(map.get("drop")),
                    map.get("drop_amount") instanceof Number amount ? amount.intValue() : 1));
        }
        return interactions;
    }

    private static int intOf(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    public boolean matches(ItemStack item) {
        if (item == null || item.getType().isAir()) return held.equalsIgnoreCase("AIR");
        var customItem = GardensFurniture.itemRegistry.getCustomItem(item);
        if (customItem != null) {
            return customItem.getIdentifier().equalsIgnoreCase(held);
        }
        return item.getType() == Material.matchMaterial(held);
    }

    /** The drop, or null when there's none (or it names nothing). */
    @Nullable
    public ItemStack buildDrop() {
        if (drop == null || dropAmount <= 0) return null;
        var item = GardensFurniture.itemRegistry.getItem(drop) != null
                ? GardensFurniture.itemRegistry.buildItem(drop)
                : Material.matchMaterial(drop) != null ? new ItemStack(Material.matchMaterial(drop)) : null;
        if (item != null) item.setAmount(dropAmount);
        return item;
    }

    public float rollPitch() {
        return (float) (1 + (ThreadLocalRandom.current().nextDouble() * 2 - 1) * soundPitchVariance);
    }

    /** A random piece to swap {@code current} for, never {@code current} itself; null when there's none. */
    @Nullable
    public String pick(String current) {
        var candidates = replaceWith.stream().filter(id -> !id.equalsIgnoreCase(current)).toList();
        if (candidates.isEmpty()) return null;
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }
}
