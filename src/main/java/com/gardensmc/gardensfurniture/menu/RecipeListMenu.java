package com.gardensmc.gardensfurniture.menu;

import com.gardensmc.gardensfurniture.recipe.GardensRecipe;
import com.gardensmc.gardensfurniture.recipe.RecipeManager;
import com.gardensmc.gardensfurniture.recipe.RecipeType;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.StaticGuiElement;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;

/** /furniture recipe: every recipe in recipes/*.yml; click one to edit it. */
public class RecipeListMenu extends Menu {

    private static final String[] LAYOUT = new String[]{
            "aaaaaaaaa",
            "aaaaaaaaa",
            "aaaaaaaaa",
            "aaaaaaaaa",
            "aaaaaaaaa",
            "b   c   n"
    };

    public RecipeListMenu(Player viewer) {
        super(LAYOUT, "Recipes", viewer);
    }

    @Override
    protected void initializeElements() {
        var group = new GuiElementGroup('a');
        RecipeManager.INSTANCE.getRecipes().values().stream()
                .sorted(Comparator.comparing(GardensRecipe::getId))
                .forEach(recipe -> group.addElement(new StaticGuiElement('a', icon(recipe), click -> {
                    new RecipeEditorMenu(viewer, recipe).open();
                    return true;
                }, describe(recipe))));
        gui.addElement(group);
        gui.addElement(new StaticGuiElement('c', new ItemStack(Material.CRAFTING_TABLE), click -> {
            promptNew(viewer);
            return true;
        }, "<green>New recipe", "<gray>You'll name it in chat"));
        gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Next page"));
        gui.addElement(new GuiPageElement('b', new ItemStack(Material.ARROW), GuiPageElement.PageAction.PREVIOUS, "Previous page"));
    }

    public static void promptNew(Player player) {
        ChatPrompt.INSTANCE.ask(player, "Type an id for the new recipe (a-z, 0-9, _):", id -> {
            id = id.toLowerCase();
            if (!GardensRecipe.isValidId(id)) {
                player.sendMessage("§cRecipe ids may only use a-z, 0-9, _ . - /");
            } else if (RecipeManager.INSTANCE.getRecipes().containsKey(id)) {
                player.sendMessage("§cThere's already a recipe called " + id + "; opening it instead");
                new RecipeEditorMenu(player, RecipeManager.INSTANCE.getRecipes().get(id)).open();
            } else {
                new RecipeEditorMenu(player, new GardensRecipe(id, RecipeType.SHAPED)).open();
            }
        });
    }

    private static ItemStack icon(GardensRecipe recipe) {
        try {
            var ref = recipe.getType() == RecipeType.FUEL ? recipe.getInput().options().getFirst() : recipe.getResult();
            var stack = RecipeManager.INSTANCE.buildItem(ref);
            if (recipe.getType() != RecipeType.FUEL) stack.setAmount(recipe.getAmount());
            return stack;
        } catch (Exception e) {
            return new ItemStack(Material.BARRIER);
        }
    }

    private static String[] describe(GardensRecipe recipe) {
        var lines = new ArrayList<String>();
        lines.add("<yellow>" + recipe.getId());
        lines.add("<gray>" + recipe.getType().configName() + (recipe.getGroup().isEmpty() ? "" : " · group " + recipe.getGroup()));
        lines.add("<dark_gray>recipes/" + recipe.getFile());
        if (!recipe.isEnabled()) lines.add("<red>Disabled");
        if (recipe.isHidden()) lines.add("<gold>Hidden from the recipe book");
        if (RecipeManager.INSTANCE.getCompiled(recipe.getId()) == null && recipe.isEnabled() && recipe.getType() != RecipeType.FUEL) {
            lines.add("<red>Failed to load, see console");
        }
        return lines.toArray(String[]::new);
    }
}
