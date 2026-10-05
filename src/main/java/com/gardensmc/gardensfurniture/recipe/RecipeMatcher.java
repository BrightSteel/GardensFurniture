package com.gardensmc.gardensfurniture.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Crafting grid matching the way vanilla does it: a shaped recipe fits anywhere in the grid and may
 * be mirrored left-to-right; a shapeless one needs exactly its ingredients in any slots.
 */
public final class RecipeMatcher {

    private RecipeMatcher() {
    }

    /** The shape with empty rows and columns around its edges removed, every row the same width. */
    public static List<String> trim(List<String> shape) {
        int width = shape.stream().mapToInt(String::length).max().orElse(0);
        int top = Integer.MAX_VALUE, bottom = -1, left = Integer.MAX_VALUE, right = -1;
        for (int y = 0; y < shape.size(); y++) {
            var row = shape.get(y);
            for (int x = 0; x < row.length(); x++) {
                if (row.charAt(x) != ' ') {
                    top = Math.min(top, y);
                    bottom = Math.max(bottom, y);
                    left = Math.min(left, x);
                    right = Math.max(right, x);
                }
            }
        }
        if (bottom < 0) return List.of();
        var trimmed = new ArrayList<String>();
        for (int y = top; y <= bottom; y++) {
            var row = String.format("%-" + width + "s", shape.get(y));
            trimmed.add(row.substring(left, right + 1));
        }
        return trimmed;
    }

    /**
     * @param shape     trimmed shape, see {@link #trim}
     * @param grid      the crafting slots row by row, null for empty
     * @param gridWidth 3 for a crafting table, 2 for the inventory grid
     */
    public static <P extends Predicate<ItemKey>> boolean matchesShaped(List<String> shape, Map<Character, P> key, ItemKey[] grid, int gridWidth) {
        int gridHeight = grid.length / gridWidth;
        int top = Integer.MAX_VALUE, bottom = -1, left = Integer.MAX_VALUE, right = -1;
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] == null) continue;
            int x = i % gridWidth, y = i / gridWidth;
            top = Math.min(top, y);
            bottom = Math.max(bottom, y);
            left = Math.min(left, x);
            right = Math.max(right, x);
        }
        if (bottom < 0 || shape.isEmpty()) return false;
        int height = shape.size(), width = shape.getFirst().length();
        if (bottom - top + 1 != height || right - left + 1 != width) return false;
        if (height > gridHeight || width > gridWidth) return false;
        return fits(shape, key, grid, gridWidth, left, top, false)
                || fits(shape, key, grid, gridWidth, left, top, true);
    }

    private static <P extends Predicate<ItemKey>> boolean fits(List<String> shape, Map<Character, P> key, ItemKey[] grid, int gridWidth, int left, int top, boolean mirrored) {
        int width = shape.getFirst().length();
        for (int y = 0; y < shape.size(); y++) {
            for (int x = 0; x < width; x++) {
                char c = shape.get(y).charAt(mirrored ? width - 1 - x : x);
                var item = grid[(top + y) * gridWidth + left + x];
                if (c == ' ') {
                    if (item != null) return false;
                } else {
                    var ingredient = key.get(c);
                    if (ingredient == null || item == null || !ingredient.test(item)) return false;
                }
            }
        }
        return true;
    }

    public static <P extends Predicate<ItemKey>> boolean matchesShapeless(List<P> ingredients, ItemKey[] grid) {
        var items = new ArrayList<ItemKey>();
        for (var item : grid) if (item != null) items.add(item);
        if (items.size() != ingredients.size()) return false;
        // ingredients can overlap ("any plank" and "oak planks"), so try every assignment; 9 slots at most
        return assign(ingredients, items, 0, new boolean[items.size()]);
    }

    private static <P extends Predicate<ItemKey>> boolean assign(List<P> ingredients, List<ItemKey> items, int index, boolean[] used) {
        if (index == ingredients.size()) return true;
        var ingredient = ingredients.get(index);
        for (int i = 0; i < items.size(); i++) {
            if (used[i] || !ingredient.test(items.get(i))) continue;
            used[i] = true;
            if (assign(ingredients, items, index + 1, used)) return true;
            used[i] = false;
        }
        return false;
    }
}
