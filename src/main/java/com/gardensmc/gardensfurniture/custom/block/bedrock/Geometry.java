package com.gardensmc.gardensfurniture.custom.block.bedrock;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class Geometry {

    private final String identifier;
    private final String cullingLayer;
    private final String culling;
    private final String cullingShape;
    private final Map<String, String> boneVisibility;

    public Geometry(ConfigurationSection geometrySection) {
        this.identifier = geometrySection.getString("identifier");
        this.cullingLayer = geometrySection.getString("culling_layer");
        this.culling = geometrySection.getString("culling");
        this.cullingShape = geometrySection.getString("culling_shape");
        var boneVisibilitySection = geometrySection.getConfigurationSection("bone_visibility");
        if (boneVisibilitySection == null) {
            this.boneVisibility = null;
        } else {
            var map = new LinkedHashMap<String, String>();
            for (var key : boneVisibilitySection.getKeys(false)) {
                map.put(key, boneVisibilitySection.getString(key));
            }
            this.boneVisibility = map;
        }
    }
}
