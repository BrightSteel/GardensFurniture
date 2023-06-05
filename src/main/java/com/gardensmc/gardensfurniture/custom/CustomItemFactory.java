package com.gardensmc.gardensfurniture.custom;

import com.gardensmc.gardensfurniture.register.CustomRegister;
import com.gardensmc.gardensfurniture.register.ItemRegistry;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;

public class CustomItemFactory {

    private CustomBlockFactory customBlockFactory;

    public CustomItemFactory (HashMap<String, String> customNoteBlocks) {
        customBlockFactory = new CustomBlockFactory(customNoteBlocks);
    }

    public CustomDirectionalItem createDirectionalItem(String identifier, String display, int custom_model, Material material, List<String> lore, ConfigurationSection section) {
        ConfigurationSection[] sections = new ConfigurationSection[] {
                section.getConfigurationSection("block-x"),
                section.getConfigurationSection("block-y"),
                section.getConfigurationSection("block-z")
        };

        CustomBlock[] customBlocks = new CustomBlock[3];
        int i = 0;
        for (ConfigurationSection block_section : sections) {
            if (block_section == null) {
                return null;
            }

            customBlocks[i] = customBlockFactory.createCustomBlock(identifier, block_section);
            i++;
        }

        return new CustomDirectionalItem(
                identifier, display, custom_model, material, lore, customBlocks[0], customBlocks[1], customBlocks[2]
        );
    }

    public CustomCaveVineItem createCaveVineItem(String identifier, String display, int custom_model, Material material, List<String> lore, ConfigurationSection section) {
        ConfigurationSection[] sections = new ConfigurationSection[] {
                section.getConfigurationSection("plant"),
                section.getConfigurationSection("vine"),
                section.getConfigurationSection("vine-25")
        };

        CustomCaveVineBlock[] customVineBlocks = new CustomCaveVineBlock[3];
        int i = 0;
        for (ConfigurationSection block_section : sections) {
            if (block_section != null) {
              CustomBlock customBlock = customBlockFactory.createCustomBlock(identifier, block_section);
              if (customBlock instanceof CustomCaveVineBlock customCaveVineBlock) {
                  customVineBlocks[i] = customCaveVineBlock;
              }
            }
            i++;
        }

        // plant and vine are required
        if (customVineBlocks[0] == null || customVineBlocks[1] == null) return null;

        return new CustomCaveVineItem(
                identifier, display, custom_model, material, lore, customVineBlocks[0], customVineBlocks[1], customVineBlocks[2]
        );
    }

    public CustomItem createCustomItem(String identifier, String display, int custom_model, Material material, List<String> lore, ConfigurationSection section) {
        CustomItem customItem = new CustomItem(identifier, display, custom_model, material, lore);

        ConfigurationSection blockSection = section.getConfigurationSection("block");
        if (blockSection != null) {
            CustomBlock customBlock = customBlockFactory.createCustomBlock(identifier, blockSection);
            customItem.setCustomBlock(customBlock);
        }
        return customItem;
    }


}
