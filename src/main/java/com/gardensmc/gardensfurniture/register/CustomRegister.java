package com.gardensmc.gardensfurniture.register;

public class CustomRegister {

    public ItemRegistry itemRegistry;

    public CustomRegister() {
        registerAll();
    }

    private void registerAll() {
        itemRegistry = new ItemRegistry();
    }

    public void reload() {
        itemRegistry.reload();
    }
}
