package com.gardensmc.gardensfurniture.custom.item.components;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public class Components {

    private final String item_model;
    private final Boolean enchantment_glint_override;
    private final Boolean glider;
    private final Boolean hide_tooltip;
    private final Integer max_stack_size;
    private final Food food;
    private final Consumable consumable;
    private final Equippable equippable;

    public Components(ConfigurationSection componentSection) {
        this.item_model = componentSection.getString("item_model");
        this.enchantment_glint_override = componentSection.contains("enchantment_glint_override")
                ? componentSection.getBoolean("enchantment_glint_override")
                : null;
        this.glider = componentSection.contains("glider")
                ? componentSection.getBoolean("glider")
                : null;
        this.hide_tooltip = componentSection.contains("hide_tooltip")
                ? componentSection.getBoolean("hide_tooltip")
                : null;
        this.max_stack_size = componentSection.contains("max_stack_size")
                ? componentSection.getInt("max_stack_size")
                : null;
        var foodSection = componentSection.getConfigurationSection("food");
        this.food = foodSection == null ? null : new Food(foodSection);
        var consumableSection = componentSection.getConfigurationSection("consumable");
        this.consumable = consumableSection == null ? null : new Consumable(consumableSection);
        var equippableSection = componentSection.getConfigurationSection("equippable");
        this.equippable = equippableSection == null ? null : new Equippable(equippableSection);
    }

}
