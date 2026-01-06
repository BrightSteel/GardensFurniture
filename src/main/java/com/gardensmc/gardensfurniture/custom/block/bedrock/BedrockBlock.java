package com.gardensmc.gardensfurniture.custom.block.bedrock;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.geysermc.geyser.api.block.custom.component.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
public class BedrockBlock {

    private final Geometry geometry;
    private final List<MaterialInstance> materialInstances;
    private final List<Integer> rotation; // size of 3
    private final BoundingBox collisionBox;
    private final BoundingBox selectionBox;

    public BedrockBlock(ConfigurationSection bedrockSection, String furnitureId) {
        var geometrySection = bedrockSection.getConfigurationSection("geometry");
        this.geometry = geometrySection == null ? null : new Geometry(geometrySection);
        var materialSections = bedrockSection.getConfigurationSection("material_instances");
        materialInstances = materialSections == null ? Collections.emptyList() : materialSections.getKeys(false)
                .stream()
                .map(key -> new MaterialInstance(key, Objects.requireNonNull(materialSections.getConfigurationSection(key), "No material section for " + furnitureId)))
                .toList();
        var rotations = bedrockSection.getIntegerList("rotation");
        if (!rotations.isEmpty() && rotations.size() != 3) {
            throw new IllegalStateException("Rotations must have length of 3!");
        }
        this.rotation = rotations;
        var collisionBoxSection = bedrockSection.getConfigurationSection("collision_box");
        this.collisionBox = collisionBoxSection == null ? null : new BoundingBox(collisionBoxSection);
        var selectionBoxSection = bedrockSection.getConfigurationSection("selection_box");
        this.selectionBox = selectionBoxSection == null ? null : new BoundingBox(selectionBoxSection);
    }

    // used by geyser
    public CustomBlockComponents toCustomBlockComponents() {
        var componentBuilder = CustomBlockComponents.builder()
                .geometry(GeometryComponent.builder()
                        .identifier(geometry == null || geometry.getIdentifier() == null ? "minecraft:geometry.full_block" : geometry.getIdentifier())
                        .build())
                .destructibleByMining(Float.MAX_VALUE)
                .placeAir(true);
        var rotation = getRotation();
        if (!rotation.isEmpty()) {
            componentBuilder.transformation(new TransformationComponent(rotation.get(0), rotation.get(1), rotation.get(2)));
        }
        var collisionBox = getCollisionBox();
        if (collisionBox != null) {
            var origin = collisionBox.getOrigin();
            var size = collisionBox.getSize();
            componentBuilder.collisionBox(new BoxComponent(origin.get(0), origin.get(1), origin.get(2), size.get(0), size.get(1), size.get(2)));
        }
        var selectionBox = getSelectionBox();
        if (selectionBox != null) {
            var origin = selectionBox.getOrigin();
            var size = selectionBox.getSize();
            componentBuilder.selectionBox(new BoxComponent(origin.get(0), origin.get(1), origin.get(2), size.get(0), size.get(1), size.get(2)));
        }
        getMaterialInstances().forEach(materialInstance -> componentBuilder.materialInstance(materialInstance.getName(), org.geysermc.geyser.api.block.custom.component.MaterialInstance.builder()
                .texture(materialInstance.getTexture())
                .renderMethod(materialInstance.getRenderMethod())
                .build()
        ));
        return componentBuilder.build();
    }
}
