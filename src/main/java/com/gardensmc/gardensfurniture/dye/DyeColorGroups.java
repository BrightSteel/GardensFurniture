package com.gardensmc.gardensfurniture.dye;

import lombok.Getter;
import org.bukkit.configuration.Configuration;

import java.util.List;
import java.util.Objects;

@Getter
public class DyeColorGroups {

    private final List<DyeColorGroup> dyeColorGroups;

    public DyeColorGroups(Configuration configuration) {
        this.dyeColorGroups = configuration.getKeys(false)
                .stream()
                .map(key -> new DyeColorGroup(Objects.requireNonNull(configuration.getConfigurationSection(key))))
                .toList();
    }
}
