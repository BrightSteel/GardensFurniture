package com.gardensmc.gardensfurniture.store;

public class FurnitureStore {

    private final String itemIdentifier;

    // optionally stored variables
    private int multiplier;
    private String facing;

    public FurnitureStore(String itemIdentifier) {
        this.itemIdentifier = itemIdentifier;
    }

    public FurnitureStore(
            String itemIdentifier,
            int multiplier,
            String facing
    ) {
        this.itemIdentifier = itemIdentifier;
        this.multiplier = multiplier;
        this.facing = facing;
    }

    public String getItemIdentifier() {
        return itemIdentifier;
    }

    public String getFacing() {
        return facing;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public static class FurnitureStoreBuilder {

        private final String itemIdentifier;

        private int multiplier;
        private String facing;

        public FurnitureStoreBuilder(String itemIdentifier) {
            this.itemIdentifier = itemIdentifier;
        }

        public FurnitureStoreBuilder withFacing(String facing) {
            this.facing = facing;
            return this;
        }

        public FurnitureStoreBuilder withMultiplier(int multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public FurnitureStore build() {
            return new FurnitureStore(
                    itemIdentifier,
                    multiplier,
                    facing
            );
        }
    }
}
