package com.gardensmc.gardensfurniture.register;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.*;
import com.gardensmc.gardensfurniture.custom.block.CustomLeavesBlock;
import com.gardensmc.gardensfurniture.custom.item.*;
import com.gardensmc.gardensfurniture.custom.item.attributes.Attributes;
import com.gardensmc.gardensfurniture.generator.GenDefinitionsHandler;
import com.gardensmc.gardensfurniture.listener.EnderStepListener;
import com.gardensmc.gardensfurniture.generator.GenOverworldDimensionHandler;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import de.tr7zw.nbtapi.NBT;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.UseCooldown;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.codehaus.plexus.util.FileUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class ItemRegistry {

    public CustomItemFactory customItemFactory;
    public CustomBlockFactory customBlockFactory;

    private final Map<String, Set<CustomItem>> customItemByCategory = new HashMap<>();
    @Getter
    private final Map<String, CustomItem> customItems = new HashMap<>();

    // also can be for leaves too: distance,persistent -> identifier
    // note,instrument,powered -> custom_item_identifier
    public final Map<String, String> customNoteBlocks = new HashMap<>();

    public static final String FURNITURE_KEY = "GardensFurnitureId";
    // keys for the interaction-range attribute modifiers applied from item components.
    private static final NamespacedKey BLOCK_INTERACTION_RANGE_KEY = new NamespacedKey("gardensfurniture", "block_interaction_range");
    private static final NamespacedKey ENTITY_INTERACTION_RANGE_KEY = new NamespacedKey("gardensfurniture", "entity_interaction_range");
    public static final File generatedDir = new File(GardensFurniture.plugin.getDataFolder(), "generated");

    // Regular expression to match text between ":" and "/"
    private static final Pattern OUTER_DIR_PATTERN = Pattern.compile("(?<=:)([^/]+)(?=/)");

    public ItemRegistry() {
        customItemFactory = new CustomItemFactory();
        customBlockFactory = new CustomBlockFactory();
    }

    public void assembleItems() {
        if (generatedDir.exists()) {
            try {
                FileUtils.cleanDirectory(generatedDir);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        Set<String> outerFolders = new HashSet<>();
        for (Map.Entry<String, Configuration> entry : ConfigUtil.all_items.entrySet()) {
            String name = entry.getKey();
            var config = entry.getValue();
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section != null) {
                    CustomItem customItem = customItemFactory.createCustomItem(key, section);
                    customItems.put(key, customItem);

                    if (customItem.showInMenu() && !customItem.isTemplate()) {
                        Set<CustomItem> categoryItems = customItemByCategory.computeIfAbsent(name, s -> new TreeSet<>());
                        categoryItems.add(customItem);
                    }

                    // write out custom models
                    var components = customItem.getComponents();
                    if (components != null && customItem.isGenerateModel()) {
                        var itemModel = components.getItem_model();
                        if (itemModel != null) {
                            try {
                                Tint tint = null;
                                if (customItem.isDyeable()) {
                                    tint = Tint.DYE;
                                } else if (customItem instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomLeavesBlock leaves && leaves.getLeavesType() == CustomLeavesBlock.LeavesType.OAK) {
                                    tint = Tint.CONSTANT;
                                }
                                writeOutModelFile(itemModel, tint, customItem.getMaterial() == Material.PLAYER_HEAD);
//                                var outerFolder = OUTER_DIR_PATTERN.matcher(itemModel);
//                                if (outerFolder.find()) {
//                                    outerFolders.add(outerFolder.group(1));
//                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        GenDefinitionsHandler.INSTANCE.generate();
        GenOverworldDimensionHandler.INSTANCE.generate();

//        if (!outerFolders.isEmpty()) {
//            writeOutAtlases(outerFolders);
//        }
    }

    @AllArgsConstructor
    @Getter
    public enum Tint {
        DYE(new ObjectMapper().createObjectNode()
                .put("type", "minecraft:dye")
                .put("default", -1)
        ),
        CONSTANT(new ObjectMapper().createObjectNode()
                .put("type", "minecraft:constant")
                .put("value", -12012264)
        );

        private final ObjectNode node;
    }

    // todo use POJOs to be cleaner
    private void writeOutModelFile(String itemModel, Tint tint, boolean isPlayerHead) throws IOException {
        JsonMapper mapper = new JsonMapper();
        var node = mapper.createObjectNode();
        var modelNode = mapper.createObjectNode();
        if (isPlayerHead) {
            modelNode.put("type", "minecraft:special");
            modelNode.put("base", itemModel);
            var subModel = mapper.createObjectNode();
            subModel.put("type", "minecraft:player_head");
            modelNode.set("model", subModel);
        } else {
            modelNode.put("type", "minecraft:model");
            modelNode.put("model", itemModel);
            if (tint != null) {
                var tints = mapper.createArrayNode();
                tints.add(tint.getNode());
                modelNode.set("tints", tints);
            }
        }
        node.set("model", modelNode);

        var output = new File(generatedDir, itemModel.replaceFirst(":", "/items/") + ".json");
        output.getParentFile().mkdirs();
        mapper.writerWithDefaultPrettyPrinter().writeValue(output, node);
    }

    private void writeOutAtlases(Set<String> outerFolders) {
        JsonMapper jsonMapper = new JsonMapper();
        var atlas = new Atlases(outerFolders
                .stream()
                .map(folder -> new Atlas("directory", folder, folder + "/"))
                .toList()
        );
        var output = new File(generatedDir, "minecraft/atlases/blocks.json");
        output.getParentFile().mkdirs();
        try {
            jsonMapper.writerWithDefaultPrettyPrinter().writeValue(output, atlas);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getItemIdentifiers() {
        return customItems.keySet().stream().toList();
    }

    @Nullable
    public Set<CustomItem> getCustomItemsByCategory(String category) {
        return customItemByCategory.get(category);
    }

    public Set<String> getItemCategories() {
        return customItemByCategory.keySet();
    }

    @Nullable
    public CustomItem getItem(String identifier) {
        return customItems.get(identifier);
    }

    @Nullable
    public ItemStack buildItem(String identifier) {
        CustomItem customItem = getItem(identifier);
        return buildItem(customItem);
    }


    @Nullable
    public ItemStack buildItem(CustomItem customItem) {
        if (customItem != null) {
            ItemStack itemStack = new ItemStack(customItem.getMaterial());
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                var customModel = customItem.getCustomModel();
                if (customModel != -1) {
                    meta.setCustomModelData(customModel);
                }
                meta.displayName(customItem.getDisplayName());

                if (customItem.getLore() != null) {
                    meta.lore(customItem.getLore()
                            .stream()
                            .map(s -> MiniMessage.miniMessage().deserialize(s))
                            .toList()
                    );
                }

                // flags
                meta.addItemFlags(customItem.getItemFlags().toArray(ItemFlag[]::new));

                // color
                if (customItem.getColor() != null && meta instanceof LeatherArmorMeta leatherArmorMeta) {
                    Color color;
                    try {
                        color = (Color) Color.class.getField(customItem.getColor().toUpperCase()).get(null);
                    } catch (Exception e) {
                        // parse as RGB
                        color = Color.fromRGB(Integer.parseInt(customItem.getColor()));
                    }
                    leatherArmorMeta.setColor(color);
                }

                if (customItem instanceof CustomPotionItem potionItem && meta instanceof PotionMeta potionMeta) {
                    if (potionItem.getPotionColor() != null) {
                        potionMeta.setColor(potionItem.getPotionColor());
                    }
                    potionMeta.setEnchantmentGlintOverride(potionItem.isGlint());
                }

                // components
                var components = customItem.getComponents();
                if (components != null) {
                    if (components.getItem_model() != null) {
                        // todo use new data builder for these, like so
                        // itemStack.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.fromString(components.getItem_model()));
                        meta.setItemModel(NamespacedKey.fromString(components.getItem_model()));
                    }
                    if (components.getGlider() != null) {
                        meta.setGlider(components.getGlider());
                    }
                    if (components.getHide_tooltip() != null) {
                        meta.setHideTooltip(components.getHide_tooltip());
                    }
                    if (components.getEnchantment_glint_override() != null) {
                        meta.setEnchantmentGlintOverride(components.getEnchantment_glint_override());
                    }
                    if (components.getMax_stack_size() != null) {
                        meta.setMaxStackSize(components.getMax_stack_size());
                    }
                    var foodComponent = components.getFood();
                    if (foodComponent != null) {
                        var food = meta.getFood();
                        food.setNutrition(foodComponent.getNutrition());
                        food.setSaturation((float) foodComponent.getSaturation());
                        food.setCanAlwaysEat(foodComponent.isCan_always_eat());
                        meta.setFood(food);
                    }
                    var consumableComponent = components.getConsumable();
                    if (consumableComponent != null) {
                        var consumable = Consumable.consumable()
                                .animation(ItemUseAnimation.valueOf(consumableComponent.getAnimation()))
                                .consumeSeconds((float) consumableComponent.getConsume_seconds());
                        itemStack.setData(DataComponentTypes.CONSUMABLE, consumable.build());
                    }
                    var equippableComponent = components.getEquippable();
                    if (equippableComponent != null) {
                        var equippable = meta.getEquippable();
                        equippable.setSlot(equippableComponent.getSlot());
                        if (equippableComponent.getModel() != null) {
                            equippable.setModel(NamespacedKey.fromString(equippableComponent.getModel()));
                        }
                        meta.setEquippable(equippable);
                    }
                    if (!components.isTooltip_display()) {
                        meta.setHideTooltip(true);
                    }
                    if (components.getTooltip_style() != null) {
                        meta.setTooltipStyle(NamespacedKey.fromString(components.getTooltip_style()));
                    }
                }

                applyAttributes(meta, customItem.getMaterial(), customItem.getAttributes());

                boolean enderStep = customItem.getMechanics() != null && customItem.getMechanics().isEnderStep();
                if (enderStep) {
                    var lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<net.kyori.adventure.text.Component>();
                    lore.add(MiniMessage.miniMessage().deserialize(
                            "<!italic><blue>+" + EnderStepListener.DISTANCE_PER_ARMOR + " Block Ender Step Range"));
                    meta.lore(lore);
                }

                itemStack.setItemMeta(meta);

                // ender_step: give the armor a shared use-cooldown group so the ability's cooldown
                // renders as the vanilla overlay on the items (triggered in EnderStepListener).
                if (enderStep) {
                    itemStack.setData(DataComponentTypes.USE_COOLDOWN, UseCooldown
                            .useCooldown(EnderStepListener.COOLDOWN_SECONDS)
                            .cooldownGroup(EnderStepListener.COOLDOWN_GROUP)
                            .build());
                }
                // paper data builder
                // need to use NMS for block_state stuff until they implement block_data builder
//                itemStack.setData(DataComponentTypes.BLOCK_DATA, new PaperBlockItemDataProperties(new BlockItemStateProperties(
//                        Map.of("fart", "toot")
//                )));
            }

            // identifying nbt
            NBT.modify(itemStack, nbt -> {
                nbt.setString(FURNITURE_KEY, customItem.getIdentifier());
            });
            return itemStack;
        }
        return null;
    }

    // Applies the item's `attributes` section (block_interaction_range / entity_interaction_range) as
    // MAIN_HAND attribute modifiers. The config value is the exact bonus added to the vanilla base
    // (block ~4.5, entity ~3); vanilla then handles the extended block/entity interaction (mining,
    // furniture-barrier left-clicks, mob attacks, the attack cooldown indicator) natively.
    private void applyAttributes(ItemMeta meta, Material material, Attributes attributes) {
        if (attributes == null) {
            return;
        }
        Double blockRange = attributes.getBlock_interaction_range();
        Double entityRange = attributes.getEntity_interaction_range();
        if (blockRange == null && entityRange == null) {
            return;
        }
        // adding any explicit attribute modifier drops the item's DEFAULT modifiers (a tool's attack
        // damage/speed), so re-add the main-hand defaults first.
        var defaults = material.getDefaultAttributeModifiers(EquipmentSlot.HAND);
        for (var entry : defaults.entries()) {
            meta.addAttributeModifier(entry.getKey(), entry.getValue());
        }
        if (blockRange != null) {
            meta.addAttributeModifier(Attribute.BLOCK_INTERACTION_RANGE, new AttributeModifier(
                    BLOCK_INTERACTION_RANGE_KEY, blockRange, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        }
        if (entityRange != null) {
            meta.addAttributeModifier(Attribute.ENTITY_INTERACTION_RANGE, new AttributeModifier(
                    ENTITY_INTERACTION_RANGE_KEY, entityRange, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        }
    }

    public void reload() {
        customItems.clear();
        customItemByCategory.clear();
        customNoteBlocks.clear();
        assembleItems();
    }

    @Nullable
    public CustomItem getCustomItem(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return null;
        var furnitureId = NBT.get(stack, nbt -> {
            return nbt.getString(FURNITURE_KEY);
        });
        return furnitureId == null ? null : customItems.get(furnitureId);
    }

    @Nullable
    public CustomItem getCustomItem(String note_block_key) {
        String identifier = customNoteBlocks.get(note_block_key);
        if (identifier != null) {
            return customItems.get(identifier);
        }
        return null;
    }
}
