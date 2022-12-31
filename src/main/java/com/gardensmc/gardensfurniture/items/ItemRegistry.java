package com.gardensmc.gardensfurniture.items;

import java.util.ArrayList;
import java.util.List;

public class ItemRegistry {

    public static ItemRegistry INSTANCE = new ItemRegistry();
    private List<CustomItem> customItems = new ArrayList<>();

    public ItemRegistry() {
        assembleItems();
    }

    private void assembleItems() {

    }
}
