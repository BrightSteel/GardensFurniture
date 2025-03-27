package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.custom.item.components.Components;
import com.github.retrooper.packetevents.protocol.recipe.CraftingCategory;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CustomItem implements Comparable<CustomItem> {

    @Getter
    private final String identifier;
    @Getter
    private final String display;
    @Getter
    private final List<String> lore;
    private final int custom_model;
    @Getter
    private final Material material;
    @Getter
    private final boolean dyeable;
    @Getter
    private final String color;
    private final boolean shardblade;
    private final CraftingCategory craft_category;
    private final boolean show_in_menu;
    @Getter
    private final Components components;
    @Getter
    private final List<ItemFlag> itemFlags;

    public CustomItem(String identifier, ConfigurationSection itemSection) {
        this.identifier = identifier;
        this.display = itemSection.getString("display", "");
        this.custom_model = itemSection.getInt("custom_model", -1);
        this.material = parseMaterial(itemSection.getString("material"));
        this.lore = itemSection.getStringList("lore");
        this.dyeable = itemSection.getBoolean("dyeable");
        this.color = itemSection.getString("color");
        this.shardblade = itemSection.getBoolean("shardblade");
        var category = itemSection.getString("craft_category");
        this.craft_category = category == null ? null : CraftingCategory.valueOf(category.toUpperCase());
        this.show_in_menu = itemSection.getBoolean("show_in_menu", true);
        var componentSection = itemSection.getConfigurationSection("components");
        this.components = componentSection == null ? null : new Components(componentSection);
        this.itemFlags = itemSection.getStringList("item_flags")
                .stream()
                .map(flag -> ItemFlag.valueOf(flag.toUpperCase()))
                .toList();
    }

    public int getCustomModel() {
        return custom_model;
    }

    public boolean isShardblade() {
        return shardblade;
    }

    public CraftingCategory getCraftingCategory() {
        return craft_category;
    }

    public boolean showInMenu() {
        return show_in_menu;
    }

    protected Material parseMaterial(String material_str) {
        Material material = Material.PAPER;
        if (material_str != null) {
            try {
                material = Material.valueOf(material_str.toUpperCase());
            } catch (Exception e) {
                return material;
            }
        }
        return material;
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public int compareTo(@NotNull CustomItem o) {
        return identifier.compareTo(o.identifier);
    }
}

