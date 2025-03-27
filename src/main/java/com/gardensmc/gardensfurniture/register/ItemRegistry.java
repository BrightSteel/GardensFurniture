package com.gardensmc.gardensfurniture.register;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.*;
import com.gardensmc.gardensfurniture.custom.block.CustomCrystalBlock;
import com.gardensmc.gardensfurniture.custom.item.*;
import com.gardensmc.gardensfurniture.util.ChatUtil;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.components.consumable.ConsumableComponent;
import org.codehaus.plexus.util.FileUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class ItemRegistry {

    public CustomItemFactory customItemFactory;
    public CustomBlockFactory customBlockFactory;

    private final Map<String, Set<CustomItem>> customItemByCategory = new HashMap<>();
    private final Map<String, CustomItem> customItems = new HashMap<>();

    // also can be for leaves too: distance,persistent -> identifier
    // note,instrument,powered -> custom_item_identifier
    public final Map<String, String> customNoteBlocks = new HashMap<>();

    public final CrystalRegistry crystalRegistry = new CrystalRegistry();

    private static final String FURNITURE_KEY = "GardensFurnitureId";
    private static final File generatedDir = new File(GardensFurniture.plugin.getDataFolder(), "generated");

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
        for (Map.Entry<String, Configuration> entry : ConfigUtil.all_items.entrySet()) {
            String name = entry.getKey();
            var config = entry.getValue();
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section != null) {
                    CustomItem customItem = customItemFactory.createCustomItem(key, section);
                    customItems.put(key, customItem);

                    registerCrystalIfPresent(customItem);

                    if (customItem.showInMenu()) {
                        Set<CustomItem> categoryItems = customItemByCategory.computeIfAbsent(name, s -> new TreeSet<>());
                        categoryItems.add(customItem);
                    }

                    // write out custom models
                    var components = customItem.getComponents();
                    if (components != null) {
                        var itemModel = components.getItem_model();
                        if (itemModel != null) {
                            try {
                                writeOutModelFile(itemModel, customItem.getMaterial().toString().toUpperCase().startsWith("LEATHER"));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    // todo use POJOs to be cleaner
    private void writeOutModelFile(String itemModel, boolean isTintable) throws IOException {
        JsonMapper mapper = new JsonMapper();
        var node = mapper.createObjectNode();
        var modelNode = mapper.createObjectNode();
        modelNode.put("type", "minecraft:model");
        modelNode.put("model", itemModel.replaceFirst(":", ":item/"));
        if (isTintable) {
            var tints = mapper.createArrayNode();
            var dyeNode = mapper.createObjectNode();
            dyeNode.put("type", "minecraft:dye");
            dyeNode.put("default", -1);
            tints.add(dyeNode);
            modelNode.set("tints", tints);
        }
        node.set("model", modelNode);

        var output = new File(generatedDir, itemModel.replaceFirst(":", "/items/") + ".json");
        output.getParentFile().mkdirs();
        mapper.writerWithDefaultPrettyPrinter().writeValue(output, node);
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
    @SuppressWarnings("UnstableApiUsage")
    public ItemStack buildItem(CustomItem customItem) {
        if (customItem != null) {
            ItemStack itemStack = new ItemStack(customItem.getMaterial());
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                var customModel = customItem.getCustomModel();
                if (customModel != -1) {
                    meta.setCustomModelData(customModel);
                }
                meta.setDisplayName(ChatColor.WHITE + ChatUtil.translateHex(customItem.getDisplay()));

                if (customItem.getLore() != null) {
                    List<String> hexLore = new ArrayList<>();
                    for (String lore : customItem.getLore()) {
                        hexLore.add(ChatUtil.translateHex(lore));
                    }
                    meta.setLore(hexLore);
                }

                // flags
                meta.addItemFlags(customItem.getItemFlags().toArray(ItemFlag[]::new));

                // color
                if (customItem.getColor() != null && meta instanceof LeatherArmorMeta leatherArmorMeta) {
                    // todo make this actually use the color with manual map of string -> color; or use rgb
                    leatherArmorMeta.setColor(Color.AQUA);
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
                        var consumable = meta.getConsumable();
                        consumable.setAnimation(ConsumableComponent.Animation.valueOf(consumableComponent.getAnimation()));
                        consumable.setConsumeSeconds(consumable.getConsumeSeconds());
                        meta.setConsumable(consumable);
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
                }

                itemStack.setItemMeta(meta);
            }

            // identifying nbt
            NBT.modify(itemStack, nbt -> {
                nbt.setString(FURNITURE_KEY, customItem.getIdentifier());
            });
            return itemStack;
        }
        return null;
    }

    public void reload() {
        customItems.clear();
        customItemByCategory.clear();
        customNoteBlocks.clear();
        crystalRegistry.clear();
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

    private void registerCrystalIfPresent(CustomItem customItem) {
        if (customItem instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomCrystalBlock crystalBlock) {
            crystalRegistry.addCrystalToMap(crystalBlock, customItem.getIdentifier());
        }
    }
}
