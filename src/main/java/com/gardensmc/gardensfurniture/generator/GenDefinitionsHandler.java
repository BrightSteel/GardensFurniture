package com.gardensmc.gardensfurniture.generator;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.register.ItemRegistry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GenDefinitionsHandler {

    public static GenDefinitionsHandler INSTANCE = new GenDefinitionsHandler();

    public void generate() {
        var models = new File(GardensFurniture.plugin.getDataFolder(), "models");
        models.mkdirs();
        var subDirectories = models.listFiles();
        if (subDirectories == null) return;

        Arrays.stream(subDirectories).forEach(subDirectory -> {
            var definition = buildYamlDefinition(subDirectory);
            if (definition != null) {
                var destination = new File(ItemRegistry.generatedDir, "definitions/" + subDirectory.getName() + ".yml");
                try {
                    definition.save(destination);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private YamlConfiguration buildYamlDefinition(File subDirectory) {
        var modelFiles = subDirectory.listFiles();
        if (modelFiles == null) return null;

        var definition = new YamlConfiguration();
        for (var modelFile : modelFiles) {
            var modelFileName = modelFile.getName().substring(0, modelFile.getName().indexOf(".json"));
            var section = definition.createSection(subDirectory.getName() + "_" + modelFileName);
            // construct modelSection
            section.set("display", StringUtils.capitalize(modelFileName.replace("_", " ")));
            var components = section.createSection("components");
            components.set("item_model", "gardens:" + subDirectory.getName() + "/" + modelFileName);
            section.set("type", "block");
            var blockSection = section.createSection("block");
            blockSection.set("type", "DISPLAY");
            var displaySection = blockSection.createSection("display_data");
            displaySection.set("face_player", true);
        }
        return definition;
    }



}
