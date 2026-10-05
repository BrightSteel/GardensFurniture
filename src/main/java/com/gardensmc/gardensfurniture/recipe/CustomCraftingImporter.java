package com.gardensmc.gardensfurniture.recipe;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigSyntax;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads CustomCrafting's recipes (plugins/CustomCrafting/data/&lt;namespace&gt;/recipes/**.conf) and its
 * custom fuels (data/&lt;namespace&gt;/items/*.conf) into GardensFurniture recipes. Nothing is written
 * here; {@link RecipeManager#saveAll} does that.
 *
 * <p>An item in CustomCrafting is a whole saved stack. One carrying a GardensFurnitureId becomes that
 * item; one with only a custom name is looked up by its name; anything else is its vanilla material.
 * CustomCrafting's checkNBT setting isn't carried over: GardensFurniture items always match by id.
 */
public class CustomCraftingImporter {

    /** How the importer sees GardensFurniture's items, so it runs without a server in tests. */
    public interface Items {
        boolean exists(String id);

        /** The id of the item whose display name, without formatting, is this; null when none or several. */
        @Nullable
        String idForDisplayName(String name);
    }

    public record Result(List<GardensRecipe> recipes, List<String> notes, List<String> skipped) {
    }

    private static final Pattern FURNITURE_ID = Pattern.compile("GardensFurnitureId:\"([^\"]+)\"");
    private static final Pattern TEXT = Pattern.compile("text:\"((?:[^\"\\\\]|\\\\.)*)\"");
    // CustomCrafting's own items, not recipes anyone wrote
    private static final Set<String> BUILT_IN = Set.of("customcrafting:advanced_crafting_table", "customcrafting:recipe_book");

    private final File root;
    private final Items items;
    private final List<String> notes = new ArrayList<>();
    private final List<String> skipped = new ArrayList<>();

    public CustomCraftingImporter(File customCraftingFolder, Items items) {
        this.root = customCraftingFolder;
        this.items = items;
    }

    public Result read() throws IOException {
        var data = new File(root, "data");
        if (!data.isDirectory()) throw new IOException("no CustomCrafting data folder at " + data);
        var disabled = readDisabled();
        var recipes = new LinkedHashMap<String, GardensRecipe>();
        var namespaces = Objects.requireNonNull(data.listFiles(File::isDirectory));
        Arrays.sort(namespaces);
        for (var namespace : namespaces) {
            var ns = namespace.getName();
            for (var file : confFiles(new File(namespace, "recipes"))) {
                var path = namespace.toPath().resolve("recipes").relativize(file.toPath()).toString()
                        .replace(File.separatorChar, '/').replaceFirst("\\.conf$", "");
                var ccKey = ns + ":" + path;
                if (BUILT_IN.contains(ccKey)) continue;
                var id = recipes.containsKey(path) ? ns + "/" + path : path;
                try {
                    var recipe = readRecipe(id, ccKey, parse(file));
                    if (recipe == null) continue;
                    if (disabled.contains("customcrafting:" + ns + "/" + path)) {
                        recipe.setEnabled(false);
                    }
                    recipes.put(id, recipe);
                } catch (Exception e) {
                    skipped.add(ccKey + ": " + e.getMessage());
                }
            }
            for (var file : confFiles(new File(namespace, "items"))) {
                var ccKey = ns + ":" + file.getName().replaceFirst("\\.conf$", "");
                if (BUILT_IN.contains(ccKey)) continue;
                try {
                    var fuel = readFuel(file.getName().replaceFirst("\\.conf$", ""), ccKey, parse(file));
                    if (fuel != null) recipes.put(fuel.getId(), fuel);
                } catch (Exception e) {
                    skipped.add(ccKey + ": " + e.getMessage());
                }
            }
        }
        return new Result(List.copyOf(recipes.values()), List.copyOf(notes), List.copyOf(skipped));
    }

    private static List<File> confFiles(File dir) throws IOException {
        if (!dir.isDirectory()) return List.of();
        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            return walk.filter(p -> p.toString().endsWith(".conf")).map(Path::toFile).sorted().toList();
        }
    }

    private static Map<String, Object> parse(File file) {
        return ConfigFactory.parseFile(file, ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF)).root().unwrapped();
    }

    private Set<String> readDisabled() {
        var config = new File(root, "config.yml");
        if (!config.exists()) return Set.of();
        var yml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(config);
        return new HashSet<>(yml.getStringList("recipes.disabled_recipes"));
    }

    @Nullable
    private GardensRecipe readRecipe(String id, String ccKey, Map<String, Object> conf) {
        var ccType = String.valueOf(conf.get("@type"));
        RecipeType type = switch (ccType) {
            case "customcrafting:crafting_shaped" -> RecipeType.SHAPED;
            case "customcrafting:crafting_shapeless" -> RecipeType.SHAPELESS;
            case "customcrafting:furnace" -> RecipeType.FURNACE;
            case "customcrafting:blast_furnace" -> RecipeType.BLASTING;
            case "customcrafting:smoker" -> RecipeType.SMOKING;
            case "customcrafting:campfire" -> RecipeType.CAMPFIRE;
            case "customcrafting:stonecutter" -> RecipeType.STONECUTTING;
            default -> throw new IllegalArgumentException("recipe type " + ccType + " isn't supported");
        };
        var recipe = new GardensRecipe(id, type);
        var group = String.valueOf(conf.getOrDefault("group", ""));
        recipe.setGroup(group);
        recipe.setHidden(Boolean.TRUE.equals(conf.get("hidden")));
        recipe.setFile("imported/" + (group.isBlank() ? "misc" : group.replaceAll("[^a-zA-Z0-9_-]", "_")) + ".yml");

        var result = map(conf.get("result"));
        var resultItems = list(result.get("items"));
        if (resultItems.isEmpty()) {
            skipped.add(ccKey + ": has no result (it never worked in CustomCrafting either)");
            return null;
        }
        if (resultItems.size() > 1) {
            notes.add(ccKey + ": picked one of %d random results; only the first was kept".formatted(resultItems.size()));
        }
        var resultItem = map(resultItems.getFirst());
        recipe.setResult(itemRef(ccKey, "result", resultItem));
        recipe.setAmount(amount(resultItem));

        switch (type) {
            case SHAPED -> readShaped(recipe, ccKey, conf);
            case SHAPELESS -> {
                for (var raw : list(conf.get("ingredients"))) {
                    recipe.getIngredients().add(ingredient(ccKey, map(raw)));
                }
            }
            default -> {
                recipe.setInput(ingredient(ccKey, map(conf.get("source"))));
                recipe.setExperience(((Number) conf.getOrDefault("exp", 0)).floatValue());
                recipe.setCookingTime(((Number) conf.getOrDefault("cookingTime", type.defaultCookingTime())).intValue());
            }
        }
        var conditions = map(conf.get("conditions"));
        if (!list(conditions.get("values")).isEmpty()) {
            notes.add(ccKey + ": had conditions (permission, world, ...) which weren't carried over");
        }
        recipe.validate();
        return recipe;
    }

    private void readShaped(GardensRecipe recipe, String ccKey, Map<String, Object> conf) {
        var ccShape = list(conf.get("shape")).stream().map(String::valueOf).toList();
        var ccIngredients = map(conf.get("ingredients"));
        int width = ccShape.stream().mapToInt(String::length).max().orElse(0);
        var grid = new Ingredient[ccShape.size() * width];
        for (int y = 0; y < ccShape.size(); y++) {
            var row = ccShape.get(y);
            for (int x = 0; x < row.length(); x++) {
                var raw = ccIngredients.get(String.valueOf(row.charAt(x)));
                if (raw != null) grid[y * width + x] = ingredient(ccKey, map(raw));
            }
        }
        recipe.setGrid(grid, width);
        var symmetry = map(conf.get("symmetry"));
        if (Boolean.TRUE.equals(symmetry.get("vertical")) || Boolean.TRUE.equals(symmetry.get("rotate"))) {
            notes.add(ccKey + ": allowed vertical flips/rotation, which crafting doesn't (left-right mirroring still works)");
        }
    }

    private Ingredient ingredient(String ccKey, Map<String, Object> ccIngredient) {
        var options = new ArrayList<ItemRef>();
        for (var raw : list(ccIngredient.get("items"))) {
            var item = map(raw);
            if (amount(item) > 1) {
                notes.add(ccKey + ": asked for %d of an item in one slot; crafting takes one per slot, so now it's 1".formatted(amount(item)));
            }
            var ref = itemRef(ccKey, "ingredient", item);
            if (!options.contains(ref)) options.add(ref);
        }
        for (var tag : list(ccIngredient.get("tags"))) {
            options.add(ItemRef.tag(String.valueOf(tag)));
        }
        if (Boolean.TRUE.equals(ccIngredient.get("allowEmpty"))) {
            notes.add(ccKey + ": had an optional (allowEmpty) slot, which is now required");
        }
        if (options.isEmpty()) throw new IllegalArgumentException("an ingredient has no items");
        return new Ingredient(options);
    }

    private ItemRef itemRef(String ccKey, String what, Map<String, Object> item) {
        var identifier = map(item.get("identifier"));
        var stack = map(identifier.get("stack"));
        if (stack.isEmpty()) {
            // the old pre-components format: item { type: GOLD_INGOT }
            var legacy = map(item.get("item"));
            if (legacy.get("type") != null) return ItemRef.parse(String.valueOf(legacy.get("type")));
            throw new IllegalArgumentException("a " + what + " isn't a saved item stack (" + identifier.getOrDefault("key", item.keySet()) + ")");
        }
        var components = map(stack.get("components"));
        var customData = components.get("minecraft:custom_data");
        if (customData != null) {
            var m = FURNITURE_ID.matcher(String.valueOf(customData));
            if (m.find()) {
                var id = m.group(1);
                if (!items.exists(id)) throw new IllegalArgumentException("GardensFurniture has no item " + id);
                return ItemRef.custom(id);
            }
        }
        var material = ItemRef.parse(String.valueOf(stack.get("id")));
        var customName = components.get("minecraft:custom_name");
        if (customName != null) {
            var name = plainText(String.valueOf(customName));
            var id = items.idForDisplayName(name);
            if (id != null) {
                notes.add(ccKey + ": the %s \"%s\" had no GardensFurniture id (an old copy of the item); matched it to %s by name".formatted(what, name, id));
                return ItemRef.custom(id);
            }
            notes.add(ccKey + ": the %s \"%s\" is a renamed %s that matches no GardensFurniture item; imported as plain %s".formatted(what, name, material, material));
        } else if (!components.isEmpty()) {
            notes.add(ccKey + ": the %s %s had extra components %s which were dropped".formatted(what, material, components.keySet()));
        }
        return material;
    }

    // {extra:[{...,text:"Lime Terracotta Shingles"}],text:""} -> Lime Terracotta Shingles
    static String plainText(String snbt) {
        var out = new StringBuilder();
        Matcher m = TEXT.matcher(snbt);
        while (m.find()) out.append(m.group(1).replace("\\\"", "\""));
        return out.toString().trim();
    }

    @Nullable
    private GardensRecipe readFuel(String id, String ccKey, Map<String, Object> conf) {
        var settings = map(conf.get("fuelSettings"));
        var burnTime = settings.get("burnTime") instanceof Number n ? n.intValue() : 0;
        if (burnTime <= 0) return null; // a custom item that isn't a fuel; nothing to import
        var recipe = new GardensRecipe(id, RecipeType.FUEL);
        recipe.setInput(Ingredient.of(itemRef(ccKey, "fuel", map(conf.get("reference")))));
        recipe.setBurnTime(burnTime);
        recipe.setFile("imported/fuels.yml");
        recipe.validate();
        return recipe;
    }

    private static int amount(Map<String, Object> item) {
        var amount = item.getOrDefault("amount", item.getOrDefault("custom_amount", 1));
        return amount instanceof Number n ? n.intValue() : 1;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static List<Object> list(Object o) {
        return o instanceof List<?> l ? new ArrayList<>(l) : List.of();
    }
}
