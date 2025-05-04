package com.gardensmc.gardensfurniture.generator.dimension;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class BiomeSource {

    private String type;
    private List<Biome> biomes;
}
