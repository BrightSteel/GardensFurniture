package com.gardensmc.gardensfurniture.register;

import com.gardensmc.gardensfurniture.custom.CustomItem;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;

public class ItemRegistry {

    private final HashMap<String, CustomItem> customItems = new HashMap<>();

    public ItemRegistry() {
        assembleItems();
    }

    private void assembleItems() {
        ConfigurationSection itemSection = ConfigUtil.glyphYaml.getConfigurationSection("items");
        if (itemSection == null) return;
        for (String key : itemSection.getKeys(true)) {
            ConfigurationSection section = itemSection.getConfigurationSection(key);
            if (section != null) {
                String display = section.getString("display");
                int custom_model = section.getInt("custom_model");
                Material material = parseMaterial(section.getString("material"));
                List<String> lore = section.getStringList("lore");

                customItems.put(key, new CustomItem(display, custom_model, material, lore));
            }
        }
    }

    private Material parseMaterial(String material_str) {
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

    public List<String> getItemIdentifiers() {
        return customItems.keySet().stream().toList();
    }

    @Nullable
    public CustomItem getItem(String identifier) {
        return customItems.get(identifier);
    }

    @Nullable
    public ItemStack buildItem(String identifier) {
        CustomItem customItem = getItem(identifier);
        if (customItem != null) {
            ItemStack itemStack = new ItemStack(customItem.getMaterial());
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(customItem.getCustomModel());
                meta.setDisplayName(customItem.getDisplay());

                if (customItem.getLore() != null) {
                    meta.setLore(customItem.getLore());
                }

                itemStack.setItemMeta(meta);
            }
            return itemStack;
        }
        return null;
    }

    public void reload() {
        customItems.clear();
        assembleItems();
    }
}
