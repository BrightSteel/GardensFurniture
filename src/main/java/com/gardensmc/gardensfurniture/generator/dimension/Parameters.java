package com.gardensmc.gardensfurniture.generator.dimension;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Parameters {

    private List<Double> continentalness;
    private JsonNode depth;
    private List<Double> erosion;
    private List<Double> humidity;
    private Double offset;
    private List<Double> temperature;
    private List<Double> weirdness;

}
