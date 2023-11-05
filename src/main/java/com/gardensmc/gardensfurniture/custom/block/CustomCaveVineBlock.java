package com.gardensmc.gardensfurniture.custom.block;

import org.bukkit.configuration.ConfigurationSection;

public class CustomCaveVineBlock extends CustomBlock {

    private final int age;

    public CustomCaveVineBlock(ConfigurationSection blockSection, String customBlockName) {
        super(blockSection);
        ConfigurationSection caveVineSection = getConfigSectionOrThrow(blockSection, "cave_vine_data", customBlockName);
        this.age = caveVineSection.getInt("age");
    }

    public int getAge() {
        return age;
    }

}
