package com.gardensmc.gardensfurniture;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

// catches plugin.yml mistakes that would otherwise only show up as "plugin failed to load" on the server
class PluginYmlTest {

    private final YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(new InputStreamReader(
            Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("plugin.yml")), StandardCharsets.UTF_8));

    @Test
    void mainClassExists() {
        var main = pluginYml.getString("main");
        assertNotNull(main);
        // look for the compiled class rather than loading it, which would need Geyser on the classpath
        assertNotNull(getClass().getClassLoader().getResource(main.replace('.', '/') + ".class"), "no class " + main);
    }

    @Test
    void commandsAreDeclared() {
        var commands = pluginYml.getConfigurationSection("commands");
        assertNotNull(commands);
        assertTrue(commands.getKeys(false).containsAll(java.util.List.of("furniture", "paint")));
    }

    // Jackson is loaded by Paper from plugin.yml libraries, not shaded into the jar
    @Test
    void jacksonIsARuntimeLibrary() {
        assertTrue(pluginYml.getStringList("libraries").stream().anyMatch(lib -> lib.startsWith("com.fasterxml.jackson.core:jackson-databind:")));
    }
}
