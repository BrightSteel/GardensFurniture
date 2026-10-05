package com.gardensmc.gardensfurniture.recipe;

import java.util.List;

/**
 * One crafting slot: any of these item references fits. Written in a recipe file as a single string,
 * or a list of strings for "any of these".
 */
public record Ingredient(List<ItemRef> options) {

    public Ingredient {
        if (options.isEmpty()) throw new IllegalArgumentException("ingredient has no items");
        options = List.copyOf(options);
    }

    public static Ingredient of(ItemRef ref) {
        return new Ingredient(List.of(ref));
    }

    public static Ingredient parse(Object raw) {
        if (raw instanceof List<?> list) {
            return new Ingredient(list.stream().map(o -> ItemRef.parse(String.valueOf(o))).toList());
        }
        if (raw == null) throw new IllegalArgumentException("missing ingredient");
        return of(ItemRef.parse(String.valueOf(raw)));
    }

    /** The form written back to yml: a string for one option, a list for several. */
    public Object toConfig() {
        if (options.size() == 1) return options.getFirst().toString();
        return options.stream().map(ItemRef::toString).toList();
    }

    @Override
    public String toString() {
        return options.size() == 1 ? options.getFirst().toString() : options.toString();
    }
}
