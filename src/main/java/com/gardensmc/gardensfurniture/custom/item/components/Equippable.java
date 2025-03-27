package com.gardensmc.gardensfurniture.custom.item.components;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EquipmentSlot;

@Getter
public class Equippable {

    private final EquipmentSlot slot;
    private final String model;

    public Equippable(ConfigurationSection equippableSection) {
        this.slot = equippableSection.contains("slot")
                ? EquipmentSlot.valueOf(equippableSection.getString("slot").toUpperCase())
                : null;
        this.model = equippableSection.getString("model");
    }
}
