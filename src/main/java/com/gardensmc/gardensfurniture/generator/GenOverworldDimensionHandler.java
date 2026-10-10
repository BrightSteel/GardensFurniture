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

    private static final long BIOME_SHUFFLE_SEED = 0x6761726465_6e73L;

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
        writeNormalPreset(dimension);
    }

    /**
     * The same overworld as the "normal" world preset (worldgen/world_preset/normal.json in the datapack), with vanilla's
     * nether and end. Worlds plugins create (resource worlds, Multiverse) take their overworld from that preset, not
     * from dimension/overworld.json, which only the main world uses.
     */
    private void writeNormalPreset(Dimension overworld) {
        var dimensions = new java.util.LinkedHashMap<String, Object>();
        dimensions.put("minecraft:overworld", overworld);
        dimensions.put("minecraft:the_end", Map.of("type", "minecraft:the_end", "generator", Map.of(
                "type", "minecraft:noise", "biome_source", Map.of("type", "minecraft:the_end"), "settings", "minecraft:end")));
        dimensions.put("minecraft:the_nether", Map.of("type", "minecraft:the_nether", "generator", Map.of(
                "type", "minecraft:noise",
                "biome_source", Map.of("type", "minecraft:multi_noise", "preset", "minecraft:nether"),
                "settings", "minecraft:nether")));
        var output = new File(generatedDir, "datapack/world_preset/normal.json");
        output.getParentFile().mkdirs();
        try {
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output, Map.of("dimensions", dimensions));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write out the normal world preset!", e);
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
        // fixed seed: the same config must produce the same datapack on every start
        Collections.shuffle(biomes, new java.util.Random(BIOME_SHUFFLE_SEED));

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
