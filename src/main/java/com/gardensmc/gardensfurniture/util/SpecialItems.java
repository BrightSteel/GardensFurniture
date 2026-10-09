package com.gardensmc.gardensfurniture.util;

import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nullable;

/**
 * Ids of the items that have behaviour in code (paint bench, maple taps, ...), from config.yml's
 * special_items section. Defaults match the ids the plugin always used.
 */
public final class SpecialItems {

    public static String PAINT_BENCH = "paint_bench";
    public static String DYE_TILE = "dye_tile";
    public static String DESTROY_STICK = "destroy_stick";
    public static String ENDER_PAD_PREFIX = "ender_pad";
    public static String MAPLE_TAP_PREFIX = "maple_tap";
    public static String MAPLE_SYRUP = "maple_syrup";
    public static String TRIPWIRE_TOP = "tripwire_top";

    private SpecialItems() {
    }

    public static void load(@Nullable ConfigurationSection section) {
        if (section == null) return;
        PAINT_BENCH = section.getString("paint_bench", PAINT_BENCH);
        DYE_TILE = section.getString("dye_tile", DYE_TILE);
        DESTROY_STICK = section.getString("destroy_stick", DESTROY_STICK);
        ENDER_PAD_PREFIX = section.getString("ender_pad_prefix", ENDER_PAD_PREFIX);
        MAPLE_TAP_PREFIX = section.getString("maple_tap_prefix", MAPLE_TAP_PREFIX);
        MAPLE_SYRUP = section.getString("maple_syrup", MAPLE_SYRUP);
        TRIPWIRE_TOP = section.getString("tripwire_top", TRIPWIRE_TOP);
    }

    public static boolean is(@Nullable CustomItem item, String id) {
        return item != null && item.getIdentifier().equals(id);
    }

    // maple taps are <prefix>_<stage>
    public static String mapleTapStage(int stage) {
        return MAPLE_TAP_PREFIX + "_" + stage;
    }
}
