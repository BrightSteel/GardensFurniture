package com.gardensmc.gardensfurniture.register;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.*;
import com.gardensmc.gardensfurniture.util.ChatUtil;
import com.gardensmc.gardensfurniture.util.ConfigUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ItemRegistry {

    private final HashMap<String, CustomItem> customItems = new HashMap<>();

    // also can be for leaves too: distance,persistent -> identifier
    // note,instrument,powered -> custom_item_identifier
    private final HashMap<String, String> customNoteBlocks = new HashMap<>();

    public ItemRegistry() {
        assembleItems();
    }

    private void assembleItems() {
        CustomItemFactory customItemFactory = new CustomItemFactory(customNoteBlocks);
        for (Configuration config : ConfigUtil.all_items) {
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section != null) {
                    String display = section.getString("display");
                    int custom_model = section.getInt("custom_model");

                    Material material = parseMaterial(section.getString("material"));
                    List<String> lore = section.getStringList("lore");
                    boolean directional = section.getBoolean("directional");
                    boolean cave_vine = section.getBoolean("cave_vine");
                    CustomItem customItem;
                    if (directional) {
                        customItem = customItemFactory.createDirectionalItem(key, display, custom_model, material, lore, section);
                    }
                    else if (cave_vine) {
                        customItem = customItemFactory.createCaveVineItem(key, display, custom_model, material, lore, section);
                    }
                    else {
                       customItem = customItemFactory.createCustomItem(key, display, custom_model, material, lore, section);
                    }

                    customItems.put(key, customItem);
                }
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

}
