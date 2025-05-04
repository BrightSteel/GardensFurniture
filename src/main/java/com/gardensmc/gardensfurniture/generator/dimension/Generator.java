package com.gardensmc.gardensfurniture.generator.dimension;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Generator {

    private String type;
    @JsonProperty("biome_source")
    private BiomeSource biomeSource;
    private String settings;
}
