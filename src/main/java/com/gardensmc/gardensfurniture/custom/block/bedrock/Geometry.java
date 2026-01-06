package com.gardensmc.gardensfurniture.custom.block.bedrock;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public class Geometry {

    private final String identifier;
    private final String culling;

    public Geometry(ConfigurationSection geometrySection) {
        this.identifier = geometrySection.getString("identifier");
        this.culling = geometrySection.getString("culling");
    }
}
