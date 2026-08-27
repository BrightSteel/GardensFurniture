package com.gardensmc.gardensfurniture.custom.block.bedrock;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public class MaterialInstance {

    private final String name;
    private final String texture;
    private final String renderMethod;
    private final boolean ambientOcclusion;
    private final boolean faceDimming;

    public MaterialInstance(String name, ConfigurationSection materialSection) {
        this.name = name;
        this.texture = materialSection.getString("texture");
        this.renderMethod = materialSection.getString("render_method") != null
                ? materialSection.getString("render_method")
                : "alpha_test";
        this.ambientOcclusion = materialSection.getBoolean("ambient_occlusion", true);
        this.faceDimming = materialSection.getBoolean("face_dimming", true);
    }
}
