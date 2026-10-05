package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.bedrock.BedrockBlock;
import com.gardensmc.gardensfurniture.custom.item.attributes.Attributes;
import com.gardensmc.gardensfurniture.custom.item.components.Components;
import com.gardensmc.gardensfurniture.custom.item.mechanics.Mechanics;
import com.github.retrooper.packetevents.protocol.recipe.CraftingCategory;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CustomItem implements Comparable<CustomItem> {

    @Getter
    private final String identifier;
    @Getter
    private final String display;
    @Getter
    private final List<String> lore;
    private final int custom_model;
    @Getter
    protected Material material;
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
    private final Attributes attributes;
    @Getter
    private final List<ItemFlag> itemFlags;
    @Getter
    private final Mechanics mechanics;
    @Getter
    private final List<AttributeModifier> attributeModifiers;
    @Getter
    private final boolean template;
    @Getter
    private final List<String> basedOn;
    @Getter
    private final String cosmeticId;
    @Getter
    private final boolean isGenerateModel;
    @Getter
    private final boolean bedrockItem;
    @Nullable
    @Getter
    private final BedrockBlock bedrockBlock;
    // vanilla item tags the item counts as, e.g. minecraft:planks: it burns and crafts like one
    @Getter
    private final List<String> tags;

    public CustomItem(String identifier, ConfigurationSection itemSection) {
        this.identifier = identifier;
        this.display = itemSection.getString("display", identifier);
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
        var attributesSection = itemSection.getConfigurationSection("attributes");
        this.attributes = attributesSection == null ? null : new Attributes(attributesSection);
        this.itemFlags = itemSection.getStringList("item_flags")
                .stream()
                .map(flag -> ItemFlag.valueOf(flag.toUpperCase()))
                .toList();
        var mechanicsSection = itemSection.getConfigurationSection("mechanics");
        this.mechanics = mechanicsSection == null ? null : new Mechanics(mechanicsSection);
        this.attributeModifiers = itemSection.getMapList("attribute_modifiers")
                .stream()
                .map(this::parseAttributeModifier)
                .toList();
        this.template = itemSection.getBoolean("template");
        this.basedOn = itemSection.getStringList("basedOn");
        this.cosmeticId = itemSection.getString("cosmetic_id");
        this.isGenerateModel = itemSection.getBoolean("generate_model", true);
        this.bedrockItem = itemSection.getBoolean("bedrock");
        var bedrockSection = itemSection.getConfigurationSection("bedrock");
        this.bedrockBlock = bedrockSection == null ? null : new BedrockBlock(bedrockSection, identifier);
        this.tags = itemSection.getStringList("tags").stream()
                .map(tag -> tag.trim().toLowerCase().replaceFirst("^#", ""))
                .map(tag -> tag.contains(":") ? tag : "minecraft:" + tag)
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
                GardensFurniture.plugin.getLogger().warning("Unknown material '%s' for item '%s', using PAPER".formatted(material_str, getIdentifier()));
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomItem that)) return false;
        return Objects.equals(identifier, that.identifier);
    }

    private AttributeModifier parseAttributeModifier(Map<?, ?> modifierMap) {
        var attribute = NamespacedKey.fromString(modifierMap.get("attribute").toString());
        var amount = Double.parseDouble(modifierMap.get("amount").toString());
        var operationNum = modifierMap.containsKey("operation")
                ? Integer.parseInt(modifierMap.get("operation").toString())
                : 0;
        var operation = Arrays.stream(AttributeModifier.Operation.values()).toList().get(operationNum);
        var slot = modifierMap.containsKey("slot")
                ? EquipmentSlotGroup.getByName(modifierMap.get("slot").toString())
                : EquipmentSlotGroup.ANY;

        return new AttributeModifier(attribute, amount, operation, slot);
    }

    public String getDisplayRawFormatted() {
        return "<!italic><white>" + StringUtils.capitalize(display) + "</white>";
    }

    public Component getDisplayName() {
        return MiniMessage.miniMessage().deserialize(getDisplayRawFormatted());
    }

    // for geyser registration
    public String getMaterialName() {
        return material.toString();
    }
}

