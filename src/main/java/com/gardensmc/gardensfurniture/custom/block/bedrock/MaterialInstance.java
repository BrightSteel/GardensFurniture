package com.gardensmc.gardensfurniture.custom.block.bedrock;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public class MaterialInstance {

    private final String name;
    private final String texture;
    private final String renderMethod;

    public MaterialInstance(String name, ConfigurationSection materialSection) {
        this.name = name;
        this.texture = materialSection.getString("texture");
        this.renderMethod = materialSection.getString("render_method") != null
                ? materialSection.getString("render_method")
                : "alpha_test";
    }
}
