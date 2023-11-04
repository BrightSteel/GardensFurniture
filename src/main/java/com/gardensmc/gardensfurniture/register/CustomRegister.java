package com.gardensmc.gardensfurniture.register;

public class CustomRegister {

    public ItemRegistry itemRegistry;
    public GlyphRegistry glyphRegistry;

    public CustomRegister() {
        registerAll();
    }

    private void registerAll() {
        itemRegistry = new ItemRegistry();
        glyphRegistry = new GlyphRegistry();
    }

    public void reload() {
        itemRegistry.reload();
        glyphRegistry.reload();
    }


}
