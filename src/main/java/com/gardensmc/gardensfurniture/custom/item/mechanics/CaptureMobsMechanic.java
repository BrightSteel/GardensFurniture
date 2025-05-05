package com.gardensmc.gardensfurniture.custom.item.mechanics;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.util.List;

@Getter
public class CaptureMobsMechanic {

    private final boolean enabled;
    private final Integer uses;
    private final List<EntityType> disallowed_types;

    public CaptureMobsMechanic(ConfigurationSection captureMobsSection) {
        this.enabled = captureMobsSection.getBoolean("enabled", true);
        this.uses = captureMobsSection.contains("uses") ? captureMobsSection.getInt("uses") : null;
        this.disallowed_types = captureMobsSection.getStringList("disallowed_types")
                .stream()
                .map(type -> EntityType.valueOf(type.toUpperCase()))
                .toList();
    }

}
