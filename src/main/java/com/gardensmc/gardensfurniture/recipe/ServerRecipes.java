package com.gardensmc.gardensfurniture.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.*;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Changes our recipes on the server in one go. Bukkit.addRecipe and removeRecipe each rebuild the
 * server's whole recipe list and re-send advancements, tags and recipes to every player, so a reload
 * of hundreds of recipes one by one would stall the server; here the list is rebuilt once.
 *
 * <p>The conversion to the server's recipes is what Paper's CraftShapedRecipe and friends do in
 * addToRecipeManager(), minus their rebuild per recipe.
 */
final class ServerRecipes {

    private ServerRecipes() {
    }

    /**
     * Takes recipes off the server without the rebuild; recipe lookups (Bukkit.getCraftingRecipe) no
     * longer find them straight away. Follow with {@link #add}, which rebuilds.
     */
    static void remove(Collection<NamespacedKey> keys) {
        var map = MinecraftServer.getServer().getRecipeManager().recipes;
        for (var key : keys) {
            map.removeRecipe(CraftNamespacedKey.toResourceKey(Registries.RECIPE, key));
        }
    }

    /**
     * Adds the recipes, then rebuilds and re-sends to players once.
     *
     * @param failed told about each recipe the server couldn't take, which is left out
     * @return the keys added
     */
    static Set<NamespacedKey> add(List<Recipe> recipes, BiConsumer<NamespacedKey, Exception> failed) {
        var manager = MinecraftServer.getServer().getRecipeManager();
        var map = manager.recipes;
        var added = new HashSet<NamespacedKey>();
        for (var recipe : recipes) {
            var key = ((Keyed) recipe).getKey();
            var resourceKey = CraftNamespacedKey.toResourceKey(Registries.RECIPE, key);
            try {
                var holder = new RecipeHolder<>(resourceKey, toServer(recipe));
                // still there after a plugin reload, when there was nothing known to remove
                map.removeRecipe(resourceKey);
                map.addRecipe(holder);
                added.add(key);
            } catch (Exception e) {
                failed.accept(key, e);
            }
        }
        // the rebuild, then advancements, tags and recipes re-sent to everyone online: once
        manager.finalizeRecipeLoading();
        return added;
    }

    private static net.minecraft.world.item.crafting.Recipe<?> toServer(Recipe recipe) {
        var info = new net.minecraft.world.item.crafting.Recipe.CommonInfo(true);
        return switch (recipe) {
            case org.bukkit.inventory.ShapedRecipe shaped -> {
                var choices = new HashMap<Character, net.minecraft.world.item.crafting.Ingredient>();
                shaped.getChoiceMap().forEach((c, choice) -> {
                    if (choice != null) choices.put(c, CraftRecipe.toIngredient(choice, false));
                });
                yield new ShapedRecipe(info, new CraftingRecipe.CraftingBookInfo(CraftRecipe.getCategory(shaped.getCategory()), shaped.getGroup()),
                        ShapedRecipePattern.of(choices, shaped.getShape()), CraftItemStack.asTemplate(shaped.getResult()));
            }
            case org.bukkit.inventory.ShapelessRecipe shapeless -> {
                var ingredients = new ArrayList<net.minecraft.world.item.crafting.Ingredient>();
                for (RecipeChoice choice : shapeless.getChoiceList()) ingredients.add(CraftRecipe.toIngredient(choice, true));
                yield new ShapelessRecipe(info, new CraftingRecipe.CraftingBookInfo(CraftRecipe.getCategory(shapeless.getCategory()), shapeless.getGroup()),
                        CraftItemStack.asTemplate(shapeless.getResult()), ingredients);
            }
            case org.bukkit.inventory.StonecuttingRecipe stonecutting -> new StonecutterRecipe(info,
                    CraftRecipe.toIngredient(stonecutting.getInputChoice(), true), CraftItemStack.asTemplate(stonecutting.getResult()));
            case org.bukkit.inventory.CookingRecipe<?> cooking -> {
                var book = new AbstractCookingRecipe.CookingBookInfo(CraftRecipe.getCategory(cooking.getCategory()), cooking.getGroup());
                var input = CraftRecipe.toIngredient(cooking.getInputChoice(), true);
                var result = CraftItemStack.asTemplate(cooking.getResult());
                float xp = cooking.getExperience();
                int time = cooking.getCookingTime();
                yield switch (cooking) {
                    case org.bukkit.inventory.FurnaceRecipe ignored -> new SmeltingRecipe(info, book, input, result, xp, time);
                    case org.bukkit.inventory.BlastingRecipe ignored -> new BlastingRecipe(info, book, input, result, xp, time);
                    case org.bukkit.inventory.SmokingRecipe ignored -> new SmokingRecipe(info, book, input, result, xp, time);
                    case org.bukkit.inventory.CampfireRecipe ignored -> new CampfireCookingRecipe(info, book, input, result, xp, time);
                    default -> throw new IllegalArgumentException("unsupported cooking recipe " + cooking.getClass().getSimpleName());
                };
            }
            default -> throw new IllegalArgumentException("unsupported recipe " + recipe.getClass().getSimpleName());
        };
    }
}
