package com.gardensmc.gardensfurniture.items;

import com.gardensmc.gardensfurniture.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nullable;
import java.util.HashMap;

public class GlyphRegistry {

    // identifier -> symbol
    private final HashMap<String, String> glyphs = new HashMap<>();

    public GlyphRegistry() {
        assembleGlyphs();
    }

    private void assembleGlyphs() {
        ConfigurationSection glyphSection = ConfigUtil.glyphYaml.getConfigurationSection("glyphs");
        if (glyphSection == null) return;

        for (String key : glyphSection.getKeys(true)) {
            ConfigurationSection section = glyphSection.getConfigurationSection(key);
            if (section != null) {
                String symbol = section.getString("symbol");
                glyphs.put(key, symbol);
            }
        }
    }

    @Nullable
    public String getGlyph(String identifier) {
        return glyphs.get(identifier);
    }

    public void reload() {
        glyphs.clear();
        assembleGlyphs();
    }
}
