package com.gardensmc.gardensfurniture.register;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.*;
import com.gardensmc.gardensfurniture.custom.block.CustomCrystalBlock;
import com.gardensmc.gardensfurniture.custom.item.CustomArmorItem;
import com.gardensmc.gardensfurniture.custom.item.CustomBlockItem;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.util.ChatUtil;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import de.tr7zw.nbtapi.NBTEntity;
import org.bukkit.*;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;

import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Level;

public class ItemRegistry {

    public CustomItemFactory customItemFactory;
    public CustomBlockFactory customBlockFactory;

    private final Map<String, Set<CustomItem>> customItemByCategory = new HashMap<>();
    private final Map<String, CustomItem> customItems = new HashMap<>();

    // also can be for leaves too: distance,persistent -> identifier
    // note,instrument,powered -> custom_item_identifier
    public final Map<String, String> customNoteBlocks = new HashMap<>();

    public final CrystalRegistry crystalRegistry = new CrystalRegistry();

    public ItemRegistry() {
        customItemFactory = new CustomItemFactory();
        customBlockFactory = new CustomBlockFactory();
    }

    public void assembleItems() {
        for (Map.Entry<String, Configuration> entry : ConfigUtil.all_items.entrySet()) {
            String name = entry.getKey();
            var config = entry.getValue();
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section != null) {
                    CustomItem customItem = customItemFactory.createCustomItem(key, section);
                    customItems.put(key, customItem);

                    registerCrystalIfPresent(customItem);

                    Set<CustomItem> categoryItems = customItemByCategory.computeIfAbsent(name, s -> new TreeSet<>());
                    categoryItems.add(customItem);
                }
            }
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
                meta.setCustomModelData(customItem.getCustomModel());
                meta.setDisplayName(ChatColor.WHITE + ChatUtil.translateHex(customItem.getDisplay()));

                if (customItem.getLore() != null) {
                    List<String> hexLore = new ArrayList<>();
                    for (String lore : customItem.getLore()) {
                        hexLore.add(ChatUtil.translateHex(lore));
                    }
                    meta.setLore(hexLore);
                }

                // color
                if (customItem.getColor() != null && meta instanceof LeatherArmorMeta leatherArmorMeta) {
                    // todo make this actually use the color with manual map of string -> color; or use rgb
                    leatherArmorMeta.setColor(Color.AQUA);
                }

                // armor
                if (customItem instanceof CustomArmorItem customArmorItem && meta instanceof ArmorMeta armorMeta) {
                    NamespacedKey key = NamespacedKey.fromString(customArmorItem.getTrimPattern());
                    if (key == null) {
                        GardensFurniture.plugin.getLogger().log(Level.SEVERE, "Failed to construct key from trim pattern: " + customArmorItem.getTrimPattern());
                        return null;
                    }
                    var trimPattern = Registry.TRIM_PATTERN.get(key);
                    if (trimPattern == null) {
                        GardensFurniture.plugin.getLogger().log(Level.SEVERE, "Failed to build armor with unknown trim pattern: " + key);
                        return null;
                    }
                    armorMeta.setTrim(new ArmorTrim(TrimMaterial.REDSTONE, trimPattern));
                }

                itemStack.setItemMeta(meta);
            }
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
        Material material = stack.getType();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return null;

        int custom_model = meta.getCustomModelData();
        return customItems.values().stream().filter(customItem -> customItem.getCustomModel() == custom_model && customItem.getMaterial() == material).findFirst().orElse(null);
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
