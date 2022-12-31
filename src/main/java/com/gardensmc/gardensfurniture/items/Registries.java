package com.gardensmc.gardensfurniture.items;

public class Registries {

    public static Registries INSTANCE;

    public ItemRegistry itemRegistry;
    public GlyphRegistry glyphRegistry;

    public Registries() {
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
