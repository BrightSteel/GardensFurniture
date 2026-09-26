package com.gardensmc.gardensfurniture.store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

// what a store-backed custom block keeps in chunk data: its item, plus facing/multiplier for items that use them
@Value
@Builder
@AllArgsConstructor
public class FurnitureStore {

    String itemIdentifier;
    int multiplier;
    String facing;

    public FurnitureStore(String itemIdentifier) {
        this(itemIdentifier, 0, null);
    }
}
