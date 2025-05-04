package com.gardensmc.gardensfurniture.generator.biomes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

@Getter
public class RangedValue {

    private final double min;
    private final double max;

    public RangedValue(ConfigurationSection section) {
        if (section == null) {
            this.min = 0;
            this.max = 0;
        } else {
            this.min = section.getDouble("min");
            this.max = section.getDouble("max");
        }
    }

    public List<Double> asDoubleList() {
        return List.of(min, max);
    }

    public ArrayNode asArrayNode() {
        var arrayNode = new ObjectMapper().createArrayNode();
        arrayNode.add(min);
        arrayNode.add(max);
        return arrayNode;
    }
}
