package com.gardensmc.gardensfurniture.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class SpecialItemsTest {

    private static final Map<String, String> DEFAULTS = Map.of(
            "paint_bench", "paint_bench",
            "dye_tile", "dye_tile",
            "destroy_stick", "destroy_stick",
            "ender_pad_prefix", "ender_pad",
            "maple_tap_prefix", "maple_tap",
            "maple_syrup", "maple_syrup",
            "tripwire_top", "tripwire_top"
    );

    // SpecialItems is static state: put the defaults back for other tests
    @AfterEach
    void restoreDefaults() {
        var config = new YamlConfiguration();
        DEFAULTS.forEach(config::set);
        SpecialItems.load(config);
    }

    // config.yml in the jar must list every setting with the id the code always used
    @Test
    void bundledConfigHasEveryDefault() {
        var stream = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("config.yml"));
        var config = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        var section = config.getConfigurationSection("special_items");
        assertNotNull(section, "config.yml needs a special_items section");
        assertEquals(DEFAULTS.keySet(), section.getKeys(false));
        DEFAULTS.forEach((key, value) -> assertEquals(value, section.getString(key), key));
    }

    @Test
    void missingSectionKeepsDefaults() {
        SpecialItems.load(null);
        assertEquals("paint_bench", SpecialItems.PAINT_BENCH);
        assertEquals("maple_tap_5", SpecialItems.mapleTapStage(5));
    }

    @Test
    void overridesAndFallsBack() {
        var config = new YamlConfiguration();
        config.set("maple_tap_prefix", "birch_tap");
        config.set("paint_bench", "easel");
        SpecialItems.load(config);
        assertEquals("easel", SpecialItems.PAINT_BENCH);
        assertEquals("birch_tap_1", SpecialItems.mapleTapStage(1));
        assertEquals("tripwire_top", SpecialItems.TRIPWIRE_TOP, "unset keys keep their default");
    }
}
