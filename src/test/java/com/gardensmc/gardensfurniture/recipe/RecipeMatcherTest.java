package com.gardensmc.gardensfurniture.recipe;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RecipeMatcherTest {

    private static final ItemKey OAK = ItemKey.vanilla(Material.OAK_PLANKS);
    private static final ItemKey BIRCH = ItemKey.vanilla(Material.BIRCH_PLANKS);
    private static final ItemKey TORCH = ItemKey.vanilla(Material.TORCH);
    private static final ItemKey PAPER = ItemKey.vanilla(Material.PAPER);
    private static final ItemKey QUILT = ItemKey.custom(Material.PAPER, "quilt_red");

    private static final ResolvedIngredient ANY_PLANK = new ResolvedIngredient(Set.of(Material.OAK_PLANKS, Material.BIRCH_PLANKS), Set.of());
    private static final ResolvedIngredient TORCH_ONLY = new ResolvedIngredient(Set.of(Material.TORCH), Set.of());
    private static final ResolvedIngredient QUILT_ONLY = new ResolvedIngredient(Set.of(), Set.of("quilt_red"));
    private static final ResolvedIngredient PAPER_ONLY = new ResolvedIngredient(Set.of(Material.PAPER), Set.of());

    @Test
    void trimRemovesEmptyEdgesAndPads() {
        assertEquals(List.of("A", "A"), RecipeMatcher.trim(List.of("   ", " A ", " A")));
        assertEquals(List.of("A ", " B"), RecipeMatcher.trim(List.of("A", " B")));
    }

    @Test
    void shapedMatchesAnywhereInTheGridAndMixesTagMembers() {
        var shape = List.of("P", "T");
        var key = Map.of('P', ANY_PLANK, 'T', TORCH_ONLY);
        assertTrue(RecipeMatcher.matchesShaped(shape, key, new ItemKey[]{
                null, null, null,
                null, null, BIRCH,
                null, null, TORCH}, 3));
        assertTrue(RecipeMatcher.matchesShaped(shape, key, new ItemKey[]{OAK, null, TORCH, null}, 2));
        // an extra item anywhere breaks it
        assertFalse(RecipeMatcher.matchesShaped(shape, key, new ItemKey[]{OAK, PAPER, TORCH, null}, 2));
        // upside down isn't it
        assertFalse(RecipeMatcher.matchesShaped(shape, key, new ItemKey[]{TORCH, null, OAK, null}, 2));
    }

    @Test
    void shapedMayBeMirroredLeftToRight() {
        var shape = List.of("PT");
        var key = Map.of('P', ANY_PLANK, 'T', TORCH_ONLY);
        assertTrue(RecipeMatcher.matchesShaped(shape, key, new ItemKey[]{TORCH, OAK, null, null}, 2));
    }

    @Test
    void customItemsAreNotTheirBaseMaterial() {
        var key = Map.of('Q', QUILT_ONLY);
        assertTrue(RecipeMatcher.matchesShaped(List.of("Q"), key, new ItemKey[]{QUILT, null, null, null}, 2));
        assertFalse(RecipeMatcher.matchesShaped(List.of("Q"), key, new ItemKey[]{PAPER, null, null, null}, 2));
        assertFalse(RecipeMatcher.matchesShapeless(List.of(PAPER_ONLY), new ItemKey[]{QUILT, null, null, null}));
    }

    @Test
    void shapelessTriesEveryAssignment() {
        var oakOnly = new ResolvedIngredient(Set.of(Material.OAK_PLANKS), Set.of());
        // greedy would give the oak to "any plank" and then have nothing for "oak"
        assertTrue(RecipeMatcher.matchesShapeless(List.of(ANY_PLANK, oakOnly), new ItemKey[]{OAK, BIRCH, null, null}));
        assertFalse(RecipeMatcher.matchesShapeless(List.of(ANY_PLANK, oakOnly), new ItemKey[]{BIRCH, BIRCH, null, null}));
        assertFalse(RecipeMatcher.matchesShapeless(List.of(QUILT_ONLY, QUILT_ONLY), new ItemKey[]{QUILT, null, null, null}));
        assertTrue(RecipeMatcher.matchesShapeless(List.of(QUILT_ONLY, QUILT_ONLY), new ItemKey[]{null, QUILT, null, QUILT}));
    }
}
