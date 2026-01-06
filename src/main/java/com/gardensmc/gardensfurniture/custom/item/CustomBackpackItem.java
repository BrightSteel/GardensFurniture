package com.gardensmc.gardensfurniture.custom.item;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public class CustomBackpackItem extends CustomItem {

    private final int rows;

    public CustomBackpackItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        this.rows = itemSection.getInt("rows", 1);
        if (rows < 1 || rows > 6) {
            throw new IllegalStateException("Rows for backpack %s must be a valid inventory size!");
        }
    }
}
