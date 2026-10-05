package com.gardensmc.gardensfurniture.recipe;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CustomCraftingImporterTest {

    // every id exists; "Lime Terracotta Shingles" is the one old copy found by its name
    private static final CustomCraftingImporter.Items ITEMS = new CustomCraftingImporter.Items() {
        @Override
        public boolean exists(String id) {
            return true;
        }

        @Override
        public String idForDisplayName(String name) {
            return name.equals("Lime Terracotta Shingles") ? "terracotta_shingles_lime" : null;
        }
    };

    private static Map<String, GardensRecipe> byId(CustomCraftingImporter.Result result) {
        return result.recipes().stream().collect(Collectors.toMap(GardensRecipe::getId, r -> r));
    }

    private static CustomCraftingImporter.Result fixtures() throws Exception {
        var root = new File(Objects.requireNonNull(CustomCraftingImporterTest.class.getClassLoader().getResource("customcrafting")).toURI());
        return new CustomCraftingImporter(root, ITEMS).read();
    }

    @Test
    void shapedCollapsesRepeatedSlotsIntoOneLetter() throws Exception {
        var lantern = byId(fixtures()).get("acacia_lantern_craft");
        assertEquals(RecipeType.SHAPED, lantern.getType());
        assertEquals(3, lantern.getShape().size());
        assertEquals(3, lantern.getKey().size()); // planks, paper, torch
        var planks = lantern.getKey().get(lantern.getShape().getFirst().charAt(0));
        assertEquals("minecraft:acacia_planks", planks.toString());
        assertEquals(ItemRef.custom("lantern_acacia"), lantern.getResult());
        assertEquals(4, lantern.getAmount());
        assertEquals("lantern", lantern.getGroup());
        assertEquals("imported/lantern.yml", lantern.getFile());
        // in CustomCrafting's disabled_recipes
        assertFalse(lantern.isEnabled());
    }

    @Test
    void importsEveryKind() throws Exception {
        var result = fixtures();
        var recipes = byId(result);

        var blanket = recipes.get("blanket_red");
        assertEquals(RecipeType.SHAPELESS, blanket.getType());
        assertEquals(2, blanket.getIngredients().size());
        assertEquals("gardens:quilt_red", blanket.getIngredients().getFirst().toString());
        assertTrue(result.notes().stream().anyMatch(n -> n.contains("blanket_red") && n.contains("2 of an item")));

        var enderite = recipes.get("blast_enderite");
        assertEquals(RecipeType.BLASTING, enderite.getType());
        assertEquals(100, enderite.getCookingTime());
        assertEquals(6.0f, enderite.getExperience());

        assertTrue(recipes.get("yellow_lamp_craft").isHidden());

        var shingles = recipes.get("lime_terracotta_shingles");
        assertEquals(ItemRef.custom("terracotta_shingles_lime"), shingles.getResult());

        var fuel = recipes.get("maple_log_fuel");
        assertEquals(RecipeType.FUEL, fuel.getType());
        assertEquals(300, fuel.getBurnTime());
        assertEquals("gardens:maple_log", fuel.getInput().toString());

        // CustomCrafting's own items aren't imported; the empty-result recipe is reported
        assertFalse(recipes.containsKey("advanced_crafting_table"));
        assertTrue(result.skipped().stream().anyMatch(s -> s.contains("iridium_smelt") && s.contains("no result")));
    }

    @Test
    void plainTextOfANameComponent() {
        assertEquals("Lime Terracotta Shingles", CustomCraftingImporter.plainText(
                "{extra:[{bold:0b,color:\"white\",italic:0b,text:\"Lime Terracotta Shingles\",underlined:0b}],text:\"\"}"));
    }

    // the real thing, when this machine has the dev server
    @Test
    void importsTheDevServer() throws Exception {
        var root = new File(System.getProperty("user.home"), "Gardens Dev/plugins/CustomCrafting");
        assumeTrue(root.isDirectory());
        var result = new CustomCraftingImporter(root, ITEMS).read();
        result.skipped().forEach(s -> System.out.println("skipped " + s));
        result.notes().forEach(n -> System.out.println("note " + n));
        System.out.println(result.recipes().size() + " imported");
        assertTrue(result.skipped().stream().allMatch(s -> s.contains("iridium_smelt")), String.valueOf(result.skipped()));
        // one of the two iridium copies has no result
        assertEquals(1, result.skipped().size());
        assertEquals(353 - 1 /* iridium */ - 2 /* CustomCrafting's own */ + 2 /* fuels */, result.recipes().size());
    }
}
