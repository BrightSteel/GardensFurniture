package com.gardensmc.gardensfurniture.recipe;

import org.bukkit.Material;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * One item reference as written in a recipe file:
 * <ul>
 *     <li>{@code minecraft:oak_planks} (or just {@code oak_planks}) - a vanilla item</li>
 *     <li>{@code gardens:maple_log} - a GardensFurniture item, by its id in items/*.yml</li>
 *     <li>{@code #minecraft:planks} - any item in a vanilla item tag</li>
 *     <li>{@code #gardens:planks} - any item in a tag from recipe_tags.yml</li>
 * </ul>
 */
public record ItemRef(Kind kind, String value) {

    public static final String CUSTOM_NAMESPACE = "gardens";

    public enum Kind { MATERIAL, CUSTOM, TAG }

    public static ItemRef material(Material material) {
        return new ItemRef(Kind.MATERIAL, material.getKey().getKey());
    }

    public static ItemRef custom(String id) {
        return new ItemRef(Kind.CUSTOM, id);
    }

    public static ItemRef tag(String namespacedKey) {
        return new ItemRef(Kind.TAG, namespacedKey.contains(":") ? namespacedKey : "minecraft:" + namespacedKey);
    }

    public static ItemRef parse(String raw) {
        var s = raw.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("empty item reference");
        if (s.startsWith("#")) {
            return tag(s.substring(1).toLowerCase(Locale.ROOT));
        }
        int colon = s.indexOf(':');
        if (colon >= 0 && s.substring(0, colon).equalsIgnoreCase(CUSTOM_NAMESPACE)) {
            // custom ids are case-sensitive, they're yml keys
            return custom(s.substring(colon + 1));
        }
        var key = (colon >= 0 ? s.substring(colon + 1) : s).toLowerCase(Locale.ROOT);
        if (colon >= 0 && !s.substring(0, colon).equalsIgnoreCase("minecraft")) {
            throw new IllegalArgumentException("unknown namespace in '" + raw + "' (use minecraft:, gardens: or #tag)");
        }
        return new ItemRef(Kind.MATERIAL, key);
    }

    /** The vanilla material, or null when this isn't a vanilla item or the name is unknown. */
    @Nullable
    public Material toMaterial() {
        if (kind != Kind.MATERIAL) return null;
        var material = Material.matchMaterial(value);
        return material != null && material.isItem() && !material.isAir() ? material : null;
    }

    @Override
    public String toString() {
        return switch (kind) {
            case MATERIAL -> "minecraft:" + value;
            case CUSTOM -> CUSTOM_NAMESPACE + ":" + value;
            case TAG -> "#" + value;
        };
    }
}
