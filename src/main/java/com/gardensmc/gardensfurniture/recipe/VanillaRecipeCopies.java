package com.gardensmc.gardensfurniture.recipe;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.*;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Copies of vanilla's recipes that take GardensFurniture items in a tag's place: with maple planks in
 * {@code minecraft:planks}, vanilla's crafting table recipe ("four of #minecraft:planks") gets a copy
 * whose slots are {@code #minecraft:planks}, which now includes maple planks.
 *
 * <p>The copies are hidden from the recipe book; vanilla's own entry is still the one players see.
 * They're never written to recipes/, and are made again on every reload.
 */
final class VanillaRecipeCopies {

    static final String ID_PREFIX = "vanilla/";

    private VanillaRecipeCopies() {
    }

    static List<GardensRecipe> make(CustomItemTags itemTags) {
        if (itemTags.usedTags().isEmpty()) return List.of();
        // the tags worth looking for, by the exact set of vanilla items they hold
        var tagsByValues = new HashMap<Set<Material>, NamespacedKey>();
        for (var key : itemTags.usedTags()) {
            var tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, key, Material.class);
            if (tag != null) tagsByValues.putIfAbsent(Set.copyOf(tag.getValues()), key);
        }
        var copies = new ArrayList<GardensRecipe>();
        for (var it = Bukkit.recipeIterator(); it.hasNext(); ) {
            var recipe = it.next();
            if (!(recipe instanceof Keyed keyed) || !keyed.getKey().getNamespace().equals(NamespacedKey.MINECRAFT)) continue;
            try {
                var copy = copy(recipe, keyed.getKey(), tagsByValues);
                if (copy != null) copies.add(copy);
            } catch (Exception ignored) {
                // a vanilla recipe this can't express (exact-item slots, a result with components): left alone
            }
        }
        return copies;
    }

    @Nullable
    private static GardensRecipe copy(Recipe recipe, NamespacedKey key, Map<Set<Material>, NamespacedKey> tagsByValues) {
        var result = recipe.getResult();
        if (result.getType().isAir() || result.hasItemMeta()) return null;
        var extended = new boolean[1];
        GardensRecipe copy;
        switch (recipe) {
            case ShapedRecipe shaped -> {
                var c = new GardensRecipe(ID_PREFIX + key.getKey(), RecipeType.SHAPED);
                for (var entry : shaped.getChoiceMap().entrySet()) {
                    if (entry.getValue() == null) continue;
                    c.getKey().put(entry.getKey(), ingredient(entry.getValue(), tagsByValues, extended));
                }
                // a shape letter with no choice is an empty slot
                var rows = new ArrayList<String>();
                for (var row : shaped.getShape()) {
                    var out = new StringBuilder();
                    for (char ch : row.toCharArray()) out.append(c.getKey().containsKey(ch) ? ch : ' ');
                    rows.add(out.toString());
                }
                c.setShape(rows);
                c.setGroup(shaped.getGroup());
                c.setCategory(shaped.getCategory().name().toLowerCase(Locale.ROOT));
                copy = c;
            }
            case ShapelessRecipe shapeless -> {
                copy = new GardensRecipe(ID_PREFIX + key.getKey(), RecipeType.SHAPELESS);
                for (var choice : shapeless.getChoiceList()) copy.getIngredients().add(ingredient(choice, tagsByValues, extended));
                copy.setGroup(shapeless.getGroup());
                copy.setCategory(shapeless.getCategory().name().toLowerCase(Locale.ROOT));
            }
            case CookingRecipe<?> cooking -> {
                var type = switch (cooking) {
                    case FurnaceRecipe ignored -> RecipeType.FURNACE;
                    case BlastingRecipe ignored -> RecipeType.BLASTING;
                    case SmokingRecipe ignored -> RecipeType.SMOKING;
                    case CampfireRecipe ignored -> RecipeType.CAMPFIRE;
                    default -> null;
                };
                if (type == null) return null;
                copy = new GardensRecipe(ID_PREFIX + key.getKey(), type);
                copy.setInput(ingredient(cooking.getInputChoice(), tagsByValues, extended));
                copy.setExperience(cooking.getExperience());
                copy.setCookingTime(cooking.getCookingTime());
                copy.setGroup(cooking.getGroup());
            }
            case StonecuttingRecipe stonecutting -> {
                copy = new GardensRecipe(ID_PREFIX + key.getKey(), RecipeType.STONECUTTING);
                copy.setInput(ingredient(stonecutting.getInputChoice(), tagsByValues, extended));
                copy.setGroup(stonecutting.getGroup());
            }
            default -> {
                return null;
            }
        }
        if (!extended[0]) return null;
        copy.setResult(ItemRef.material(result.getType()));
        copy.setAmount(result.getAmount());
        copy.setHidden(true);
        copy.setVanillaCopy(true);
        copy.validate();
        return copy;
    }

    private static Ingredient ingredient(RecipeChoice choice, Map<Set<Material>, NamespacedKey> tagsByValues, boolean[] extended) {
        if (!(choice instanceof RecipeChoice.MaterialChoice materials)) {
            throw new IllegalArgumentException("not a material choice");
        }
        var tag = tagsByValues.get(Set.copyOf(materials.getChoices()));
        if (tag != null) {
            extended[0] = true;
            return Ingredient.of(ItemRef.tag(tag.toString()));
        }
        return new Ingredient(materials.getChoices().stream().map(ItemRef::material).toList());
    }
}
