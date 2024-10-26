package com.gardensmc.gardensfurniture.custom.item;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

public class CustomArmorItem extends CustomItem {

    private final String trimPattern;

    public CustomArmorItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        var armorSection = itemSection.getConfigurationSection("armor");
        this.trimPattern = Objects.requireNonNull(armorSection.getString("trim_pattern"));
    }

    public String getTrimPattern() {
        return trimPattern;
    }
}
