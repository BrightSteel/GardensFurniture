package com.gardensmc.gardensfurniture.custom.item.mechanics;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public class Mechanics {

    private final String stripped_log;
    private final String sapling_structure;
    private final String fungus_structure;
    private final CaptureMobsMechanic capture_mobs;

    public Mechanics(ConfigurationSection mechanicsSection) {
        this.stripped_log = mechanicsSection.getString("stripped_log");
        this.sapling_structure = mechanicsSection.getString("sapling_structure");
        this.fungus_structure = mechanicsSection.getString("fungus_structure");
        var captureMobsSection = mechanicsSection.getConfigurationSection("capture_mobs");
        this.capture_mobs = captureMobsSection != null ? new CaptureMobsMechanic(captureMobsSection) : null;
    }

    public boolean isCaptureMobs() {
        return capture_mobs != null && capture_mobs.isEnabled();
    }
}
