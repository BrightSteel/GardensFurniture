package com.gardensmc.gardensfurniture.custom.item.attributes;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

/**
 * The {@code attributes} section of a custom item - vanilla attribute-modifier bonuses applied to the
 * item (see {@code ItemRegistry.applyAttributes}). Values are the exact bonus added to the vanilla
 * base for that attribute.
 */
@Getter
public class Attributes {

    // interaction-range bonuses (blocks), added to the vanilla base (block ~4.5, entity ~3).
    // null = not set. Applied as MAIN_HAND attribute modifiers.
    private final Double block_interaction_range;
    private final Double entity_interaction_range;

    public Attributes(ConfigurationSection attributesSection) {
        this.block_interaction_range = attributesSection.contains("block_interaction_range")
                ? attributesSection.getDouble("block_interaction_range")
                : null;
        this.entity_interaction_range = attributesSection.contains("entity_interaction_range")
                ? attributesSection.getDouble("entity_interaction_range")
                : null;
    }
}
