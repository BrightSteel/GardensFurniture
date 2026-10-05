package com.gardensmc.gardensfurniture.recipe;

import java.util.Locale;

public enum RecipeType {
    SHAPED,
    SHAPELESS,
    FURNACE(200),
    BLASTING(100),
    SMOKING(100),
    CAMPFIRE(600),
    STONECUTTING,
    // not a recipe: makes `input` burn in furnaces for `burn_time` ticks
    FUEL;

    private final int defaultCookingTime;

    RecipeType() {
        this(0);
    }

    RecipeType(int defaultCookingTime) {
        this.defaultCookingTime = defaultCookingTime;
    }

    public int defaultCookingTime() {
        return defaultCookingTime;
    }

    public boolean isCrafting() {
        return this == SHAPED || this == SHAPELESS;
    }

    public boolean isCooking() {
        return defaultCookingTime > 0;
    }

    /** Recipes with a single `input` item rather than a grid. */
    public boolean hasInput() {
        return isCooking() || this == STONECUTTING || this == FUEL;
    }

    public String configName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static RecipeType parse(String raw) {
        var s = raw.trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            // the names vanilla's recipe json uses
            case "CRAFTING_SHAPED" -> SHAPED;
            case "CRAFTING_SHAPELESS" -> SHAPELESS;
            case "SMELTING" -> FURNACE;
            case "BLAST_FURNACE", "BLAST" -> BLASTING;
            case "SMOKER" -> SMOKING;
            case "CAMPFIRE_COOKING" -> CAMPFIRE;
            case "STONECUTTER" -> STONECUTTING;
            default -> valueOf(s);
        };
    }
}
