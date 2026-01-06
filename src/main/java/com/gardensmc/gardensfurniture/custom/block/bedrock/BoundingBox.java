package com.gardensmc.gardensfurniture.custom.block.bedrock;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

@Getter
public class BoundingBox {

    private final List<Float> origin;
    private final List<Float> size;

    public BoundingBox(ConfigurationSection boundingBoxSection) {
        var origins = boundingBoxSection.getFloatList("origin");
        if (origins.isEmpty()) {
            origin = List.of(-8f, 0f, -8f);
        } else if (origins.size() != 3) {
            throw new IllegalStateException("Origin must have length of 3!");
        } else {
            this.origin = origins;
        }

        var sizes = boundingBoxSection.getFloatList("size");
        if (sizes.isEmpty()) {
            size = List.of(16f, 16f, 16f);
        } else if (sizes.size() != 3) {
            throw new IllegalStateException("Size must have length of 3!");
        } else {
            this.size = sizes;
        }
    }
}
