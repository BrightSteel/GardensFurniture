package com.gardensmc.gardensfurniture.store;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FurnitureStoreTest {

    @Test
    void identifierOnlyHasNoFacingOrMultiplier() {
        var store = new FurnitureStore("maple_log");
        assertEquals("maple_log", store.getItemIdentifier());
        assertEquals(0, store.getMultiplier());
        assertNull(store.getFacing());
    }

    @Test
    void builderMatchesConstructor() {
        var built = FurnitureStore.builder().itemIdentifier("clover").multiplier(3).facing("NORTH").build();
        assertEquals(new FurnitureStore("clover", 3, "NORTH"), built);
    }

    @Test
    void builderDefaults() {
        assertEquals(new FurnitureStore("x"), FurnitureStore.builder().itemIdentifier("x").build());
    }
}
