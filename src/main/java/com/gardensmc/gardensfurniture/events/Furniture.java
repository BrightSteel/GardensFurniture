package com.gardensmc.gardensfurniture.events;

import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;

public record Furniture(CustomItem customItem, ItemDisplay itemDisplay, Interaction interaction) {
}
