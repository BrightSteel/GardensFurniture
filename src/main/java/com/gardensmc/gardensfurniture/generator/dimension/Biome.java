package com.gardensmc.gardensfurniture.generator.dimension;

import com.fasterxml.jackson.databind.node.DoubleNode;
import com.gardensmc.gardensfurniture.generator.biomes.CustomBiome;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Biome {

    private String biome;
    private Parameters parameters;

    public static Biome createBiome(CustomBiome customBiome) {
        var biome = new Biome();
        biome.setBiome(customBiome.getBiomeName());
        var settings = customBiome.getAddSettings();
        var parameters = new Parameters();
        parameters.setContinentalness(settings.getContinentalness().asDoubleList());
        parameters.setErosion(settings.getErosion().asDoubleList());
        parameters.setHumidity(settings.getHumidity().asDoubleList());
        parameters.setTemperature(settings.getTemperature().asDoubleList());
        parameters.setWeirdness(settings.getWeirdness().asDoubleList());
        parameters.setDepth(new DoubleNode(settings.getDepth()));
        parameters.setOffset(0.0);
        biome.setParameters(parameters);
        return biome;
    }
}
