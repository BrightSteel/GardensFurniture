package com.gardensmc.gardensfurniture.recipe;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GardensRecipeTest {

    private static GardensRecipe load(String yml, String id) throws Exception {
        var config = new YamlConfiguration();
        config.loadFromString(yml);
        return GardensRecipe.fromConfig(id, config.getConfigurationSection(id));
    }

    @Test
    void readsShapedWithTagsAndAlternatives() throws Exception {
        var recipe = load("""
                lantern:
                  type: shaped
                  group: lantern
                  shape: ["PPP", "PTP", "PPP"]
                  key:
                    P: "#minecraft:planks"
                    T: [minecraft:torch, soul_torch, gardens:fancy_torch]
                  result: gardens:lantern_acacia
                  amount: 4
                """, "lantern");
        assertEquals(RecipeType.SHAPED, recipe.getType());
        assertEquals(new ItemRef(ItemRef.Kind.TAG, "minecraft:planks"), recipe.getKey().get('P').options().getFirst());
        assertEquals(List.of(ItemRef.parse("minecraft:torch"), new ItemRef(ItemRef.Kind.MATERIAL, "soul_torch"), ItemRef.custom("fancy_torch")),
                recipe.getKey().get('T').options());
        assertEquals(ItemRef.custom("lantern_acacia"), recipe.getResult());
        assertEquals(4, recipe.getAmount());
    }

    @Test
    void roundTripsEveryType() throws Exception {
        var yml = """
                a:
                  type: shaped
                  category: equipment
                  copy_contents: true
                  shape: ["XY"]
                  key: {X: minecraft:stick, Y: "#gardens:logs"}
                  result: minecraft:torch
                b:
                  type: shapeless
                  hidden: true
                  ingredients: [gardens:quilt_red, gardens:quilt_red]
                  result: gardens:blanket_red
                  amount: 3
                c:
                  type: blasting
                  enabled: false
                  input: gardens:enderite_ore
                  experience: 6.0
                  cooking_time: 100
                  result: gardens:enderite
                d:
                  type: fuel
                  input: gardens:maple_log
                  burn_time: 300
                """;
        var config = new YamlConfiguration();
        config.loadFromString(yml);
        var out = new YamlConfiguration();
        for (var id : config.getKeys(false)) {
            GardensRecipe.fromConfig(id, config.getConfigurationSection(id)).writeTo(out.createSection(id));
        }
        // and again, which must give the same thing
        var again = new YamlConfiguration();
        for (var id : out.getKeys(false)) {
            GardensRecipe.fromConfig(id, out.getConfigurationSection(id)).writeTo(again.createSection(id));
        }
        assertEquals(out.saveToString(), again.saveToString());
        assertFalse(out.getBoolean("c.enabled"));
        assertTrue(out.getBoolean("b.hidden"));
        assertEquals(300, out.getInt("d.burn_time"));
        assertEquals("#gardens:logs", out.getString("a.key.Y"));
        assertEquals("equipment", out.getString("a.category"));
        assertTrue(out.getBoolean("a.copy_contents"));
    }

    @Test
    void rejectsMistakes() {
        assertThrows(IllegalArgumentException.class, () -> load("""
                x:
                  type: shaped
                  shape: ["AB"]
                  key: {A: stick}
                  result: torch
                """, "x"));
        assertThrows(IllegalArgumentException.class, () -> load("""
                x:
                  type: shapeless
                  ingredients: [stick]
                  result: "#minecraft:planks"
                """, "x"));
        assertThrows(IllegalArgumentException.class, () -> load("""
                x:
                  type: shapeless
                  ingredients: [stick]
                  result: other:thing
                """, "x"));
        assertThrows(IllegalArgumentException.class, () -> new GardensRecipe("Bad Id", RecipeType.SHAPED));
        assertThrows(IllegalArgumentException.class, () -> load("""
                x:
                  type: shapeless
                  category: food
                  ingredients: [stick]
                  result: torch
                """, "x"));
    }
}
