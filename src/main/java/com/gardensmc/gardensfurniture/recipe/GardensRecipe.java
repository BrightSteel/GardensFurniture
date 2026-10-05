package com.gardensmc.gardensfurniture.recipe;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Pattern;

/**
 * A recipe as written in plugins/GardensFurniture/recipes/*.yml. Each top-level key in those files is
 * a recipe id:
 * <pre>
 * lantern_acacia:
 *   type: shaped          # shaped, shapeless, furnace, blasting, smoking, campfire, stonecutting, fuel
 *   group: lantern        # optional: recipe book groups recipes that share a group
 *   shape: ["AAA", "PTP", "AAA"]
 *   key:
 *     A: "#minecraft:planks"     # any plank
 *     P: minecraft:paper
 *     T: [minecraft:torch, minecraft:soul_torch]   # either torch
 *   result: gardens:lantern_acacia
 *   amount: 4
 * </pre>
 * shapeless recipes list {@code ingredients}; furnace-like and stonecutting recipes take one
 * {@code input} (plus {@code experience} and {@code cooking_time} in ticks); fuel takes {@code input}
 * and {@code burn_time} in ticks. Optional on all: {@code enabled: false} turns a recipe off,
 * {@code hidden: true} keeps it out of the recipe book, and {@code copy_contents: true} (crafting only)
 * moves the items inside an ingredient, like a filled shulker box, into the result. {@code category}
 * picks the recipe book tab: building, redstone, equipment or misc for crafting; food, blocks or misc
 * for a furnace, blocks or misc for a blast furnace (misc when left out; a smoker only has food).
 */
@Getter
@Setter
public class GardensRecipe {

    private static final Pattern ID = Pattern.compile("[a-z0-9_.\\-/]+");

    private final String id;
    private RecipeType type;
    private String group = "";
    private boolean enabled = true;
    private boolean hidden = false;

    // shaped
    private List<String> shape = new ArrayList<>();
    private Map<Character, Ingredient> key = new LinkedHashMap<>();
    // shapeless
    private List<Ingredient> ingredients = new ArrayList<>();
    // cooking, stonecutting, fuel
    @Nullable
    private Ingredient input;

    @Nullable
    private ItemRef result;
    private int amount = 1;
    private float experience = 0;
    private int cookingTime;
    private int burnTime;
    private boolean copyContents = false;
    // recipe book tab, lowercase; null is misc
    @Nullable
    private String category;

    // made from a vanilla recipe for items with tags: (see VanillaRecipeCopies), never saved
    private boolean vanillaCopy = false;

    // the file under recipes/ it was read from, written back to on save
    @Nullable
    private String file;

    public GardensRecipe(String id, RecipeType type) {
        if (!isValidId(id)) {
            throw new IllegalArgumentException("recipe id '" + id + "' may only use a-z, 0-9, _ . - /");
        }
        this.id = id;
        this.type = type;
        this.cookingTime = type.defaultCookingTime();
    }

    public static boolean isValidId(String id) {
        return id != null && ID.matcher(id).matches();
    }

    public static GardensRecipe fromConfig(String id, ConfigurationSection section) {
        var typeName = section.getString("type");
        if (typeName == null) throw new IllegalArgumentException("missing type");
        var recipe = new GardensRecipe(id, RecipeType.parse(typeName));
        var type = recipe.type;
        recipe.group = section.getString("group", "");
        recipe.enabled = section.getBoolean("enabled", true);
        recipe.hidden = section.getBoolean("hidden", false);
        recipe.amount = section.getInt("amount", 1);
        recipe.copyContents = section.getBoolean("copy_contents", false);
        var category = section.getString("category");
        recipe.category = category == null ? null : category.toLowerCase(Locale.ROOT);

        if (type != RecipeType.FUEL) {
            var result = section.getString("result");
            if (result == null) throw new IllegalArgumentException("missing result");
            recipe.result = ItemRef.parse(result);
            if (recipe.result.kind() == ItemRef.Kind.TAG) throw new IllegalArgumentException("result can't be a tag");
            if (recipe.amount < 1 || recipe.amount > 99) throw new IllegalArgumentException("amount must be 1-99");
        }

        switch (type) {
            case SHAPED -> {
                recipe.shape = new ArrayList<>(section.getStringList("shape"));
                var keySection = section.getConfigurationSection("key");
                if (keySection == null) throw new IllegalArgumentException("missing key");
                for (var k : keySection.getKeys(false)) {
                    if (k.length() != 1 || k.equals(" ")) throw new IllegalArgumentException("key '" + k + "' must be one character");
                    recipe.key.put(k.charAt(0), Ingredient.parse(keySection.get(k)));
                }
            }
            case SHAPELESS -> {
                for (var raw : Objects.requireNonNullElse(section.getList("ingredients"), List.of())) {
                    recipe.ingredients.add(Ingredient.parse(raw));
                }
            }
            default -> {
                recipe.input = Ingredient.parse(section.get("input"));
                recipe.experience = (float) section.getDouble("experience", 0);
                recipe.cookingTime = section.getInt("cooking_time", type.defaultCookingTime());
                recipe.burnTime = section.getInt("burn_time", 0);
            }
        }
        recipe.validate();
        return recipe;
    }

    /** Throws when the recipe couldn't be registered, with a message fit for the console. */
    public void validate() {
        if (copyContents && !type.isCrafting()) throw new IllegalArgumentException("copy_contents only works for crafting recipes");
        if (category != null && !categories(type).contains(category)) {
            throw new IllegalArgumentException("category for " + type.configName() + " must be one of " + categories(type));
        }
        switch (type) {
            case SHAPED -> {
                if (shape.isEmpty() || shape.size() > 3) throw new IllegalArgumentException("shape needs 1-3 rows");
                int width = shape.stream().mapToInt(String::length).max().orElse(0);
                if (width == 0 || width > 3) throw new IllegalArgumentException("shape rows need 1-3 characters");
                boolean any = false;
                for (var row : shape) {
                    for (char c : row.toCharArray()) {
                        if (c == ' ') continue;
                        if (!key.containsKey(c)) throw new IllegalArgumentException("shape uses '" + c + "' but key doesn't define it");
                        any = true;
                    }
                }
                if (!any) throw new IllegalArgumentException("shape is empty");
            }
            case SHAPELESS -> {
                if (ingredients.isEmpty() || ingredients.size() > 9) throw new IllegalArgumentException("shapeless needs 1-9 ingredients");
            }
            case FUEL -> {
                if (input == null) throw new IllegalArgumentException("missing input");
                if (burnTime <= 0) throw new IllegalArgumentException("burn_time must be more than 0");
            }
            default -> {
                if (input == null) throw new IllegalArgumentException("missing input");
                if (type.isCooking() && cookingTime <= 0) throw new IllegalArgumentException("cooking_time must be more than 0");
            }
        }
    }

    /** Writes the recipe's fields into an empty section, leaving defaults out. */
    public void writeTo(ConfigurationSection section) {
        section.set("type", type.configName());
        if (!group.isEmpty()) section.set("group", group);
        if (!enabled) section.set("enabled", false);
        if (hidden) section.set("hidden", true);
        if (copyContents) section.set("copy_contents", true);
        if (category != null) section.set("category", category);
        switch (type) {
            case SHAPED -> {
                section.set("shape", shape);
                var keySection = section.createSection("key");
                key.forEach((c, ingredient) -> keySection.set(String.valueOf(c), ingredient.toConfig()));
            }
            case SHAPELESS -> section.set("ingredients", ingredients.stream().map(Ingredient::toConfig).toList());
            default -> {
                section.set("input", input == null ? null : input.toConfig());
                if (type.isCooking()) {
                    if (experience != 0) section.set("experience", (double) experience);
                    section.set("cooking_time", cookingTime);
                }
                if (type == RecipeType.FUEL) section.set("burn_time", burnTime);
            }
        }
        if (type != RecipeType.FUEL) {
            section.set("result", result == null ? null : result.toString());
            if (amount != 1) section.set("amount", amount);
        }
    }

    /** The recipe book tabs a type can go in; empty when the type has no tabs. */
    public static List<String> categories(RecipeType type) {
        // the tabs vanilla's book has for each block; campfires and stonecutters have no book
        return switch (type) {
            case SHAPED, SHAPELESS -> List.of("building", "redstone", "equipment", "misc");
            case FURNACE -> List.of("food", "blocks", "misc");
            case BLASTING -> List.of("blocks", "misc");
            case SMOKING -> List.of("food");
            default -> List.of();
        };
    }

    /**
     * Sets a shaped recipe's shape and key from a grid of slots (null for empty), row by row. The
     * same ingredient in several slots gets one letter, named after the item where it can be.
     */
    public void setGrid(Ingredient[] grid, int width) {
        var letters = new LinkedHashMap<String, Character>();
        var newKey = new LinkedHashMap<Character, Ingredient>();
        var rows = new ArrayList<String>();
        for (int y = 0; y * width < grid.length; y++) {
            var row = new StringBuilder();
            for (int x = 0; x < width; x++) {
                var ingredient = grid[y * width + x];
                if (ingredient == null) {
                    row.append(' ');
                    continue;
                }
                var letter = letters.computeIfAbsent(String.valueOf(ingredient.toConfig()), k -> pickLetter(ingredient, newKey.keySet()));
                newKey.putIfAbsent(letter, ingredient);
                row.append(letter);
            }
            rows.add(row.toString());
        }
        this.shape = new ArrayList<>(RecipeMatcher.trim(rows));
        this.key = newKey;
    }

    private static char pickLetter(Ingredient ingredient, Set<Character> taken) {
        var name = ingredient.options().getFirst().value();
        name = name.substring(name.indexOf(':') + 1);
        // "acacia_planks" -> P(lanks), A(cacia), then any other letter in it, then the alphabet
        var candidates = new StringBuilder();
        var words = name.split("[_/]");
        for (int i = words.length - 1; i >= 0; i--) if (!words[i].isEmpty()) candidates.append(words[i].charAt(0));
        candidates.append(name.replaceAll("[^a-z]", "")).append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        for (char c : candidates.toString().toUpperCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetter(c) && !taken.contains(c)) return c;
        }
        for (char c = '0'; c <= '9'; c++) if (!taken.contains(c)) return c;
        throw new IllegalStateException("out of letters");
    }

    /** Every ingredient the recipe uses, in no particular order. */
    public List<Ingredient> allIngredients() {
        return switch (type) {
            case SHAPED -> {
                // only the ones the shape actually uses
                var used = new ArrayList<Ingredient>();
                for (var row : shape) for (char c : row.toCharArray()) if (c != ' ') used.add(key.get(c));
                yield used;
            }
            case SHAPELESS -> ingredients;
            default -> input == null ? List.of() : List.of(input);
        };
    }
}
