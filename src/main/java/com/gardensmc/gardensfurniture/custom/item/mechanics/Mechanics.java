package com.gardensmc.gardensfurniture.custom.item.mechanics;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public class Mechanics {

    private final String stripped_log;
    private final String sapling_structure;

    public Mechanics(ConfigurationSection mechanicsSection) {
        this.stripped_log = mechanicsSection.getString("stripped_log");
        this.sapling_structure = mechanicsSection.getString("sapling_structure");
    }
}
