package com.gardensmc.gardensfurniture.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.generator.biomes.CustomBiome;
import com.gardensmc.gardensfurniture.generator.dimension.Biome;
import com.gardensmc.gardensfurniture.generator.dimension.BiomeSource;
import com.gardensmc.gardensfurniture.generator.dimension.Dimension;
import com.gardensmc.gardensfurniture.generator.dimension.Generator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.gardensmc.gardensfurniture.register.ItemRegistry.generatedDir;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GenOverworldDimensionHandler {

    public static final GenOverworldDimensionHandler INSTANCE = new GenOverworldDimensionHandler();

    public void generate() {
        // construct dimension json
        var dimension = new Dimension();
        dimension.setType("minecraft:overworld");
        var generator = new Generator();
        dimension.setGenerator(generator);
        generator.setType("minecraft:noise");
        generator.setSettings("minecraft:overworld");
        BiomeSource overworldBiomeSrc;
        try {
            overworldBiomeSrc = parseVanillaOverworld();
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse overworld biome source!", e);
        }
        generator.setBiomeSource(overworldBiomeSrc);

        // add custom biomes
        applyBiomeModifications(dimension, parseCustomBiomes());

        // write out file
        var output = new File(generatedDir, "datapack/overworld.json");
        output.getParentFile().mkdirs();
        try {
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output, dimension);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write out overworld dimension file!");
        }
    }

    public BiomeSource parseVanillaOverworld() throws IOException {
        var vanilla = loadFile("overworld.json");
        var mapper = new ObjectMapper();
        var biomeSource = mapper.readValue(vanilla, BiomeSource.class);
        biomeSource.setType("minecraft:multi_noise");
        return biomeSource;
    }

    public void applyBiomeModifications(Dimension dimension, List<CustomBiome> customBiomes) {
        var biomes = dimension.getGenerator().getBiomeSource().getBiomes();
        Collections.shuffle(biomes);

        // apply replacements
        customBiomes.stream()
                .filter(customBiome -> customBiome.getModifier() == CustomBiome.Modifier.REPLACE)
                .forEach(customBiome -> {
                    var replaceSettings = customBiome.getReplaceSettings();
                    var replacedCount = 0;
                    for (var biome : biomes) {
                        if (replacedCount >= replaceSettings.getCount()) {
                            break;
                        }
                        if (biome.getBiome().equalsIgnoreCase(replaceSettings.getVanilla())) {
                            var depthFilter = replaceSettings.getDepthFilter();
                            if (depthFilter == null || biome.getParameters().getDepth().asDouble() == depthFilter) {
                                biome.setBiome(customBiome.getBiomeName());
                                replacedCount++;
                            }
                        }
                    }
                });

        // apply adds
        customBiomes.stream()
                .filter(customBiome -> customBiome.getModifier() == CustomBiome.Modifier.ADD)
                .forEach(customBiome -> biomes.addAll(
                        Collections.nCopies(customBiome.getAddSettings().getCount(), Biome.createBiome(customBiome)))
                );

    }

    public List<CustomBiome> parseCustomBiomes() {
        var biomesFile = YamlConfiguration.loadConfiguration(loadFile("biomes.yml"));
        return biomesFile.getKeys(false)
                .stream()
                .map(key -> new CustomBiome(key, Objects.requireNonNull(biomesFile.getConfigurationSection(key))))
                .toList();
    }

    private File loadFile(String path) {
        var file = new File(GardensFurniture.plugin.getDataFolder(), path);
        if (!file.exists()) {
            try (var resource = getClass().getClassLoader().getResourceAsStream(path)) {
                if (resource != null) {
                    Files.copy(resource, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    throw new IllegalStateException("No resource found for " + path);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return file;
    }
}
