package com.gardensmc.gardensfurniture.menu;

import com.gardensmc.gardensfurniture.recipe.*;
import com.gardensmc.gardensfurniture.util.ItemUtil;
import de.themoep.inventorygui.DynamicGuiElement;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.StaticGuiElement;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds or edits one recipe. The grid and result hold copies: click a slot with an item to put a
 * copy there (your item stays on the cursor), click it empty-handed to clear it, right-click it to
 * cycle what the slot accepts: that exact item, or any item in one of its tags ("any plank").
 */
public class RecipeEditorMenu extends Menu {

    private static final String[] LAYOUT = new String[]{
            "         ",
            " ggg     ",
            " ggg   r ",
            " ggg     ",
            "t a c x k",
            "bhpey  ds"
    };
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final String id;
    @Nullable
    private final String file;
    private final boolean exists;

    private RecipeType type;
    private final ItemStack[] grid = new ItemStack[9];
    private final Ingredient[] ingredients = new Ingredient[9];
    // what each slot held when the recipe was opened, kept as a cycle option so a list isn't lost
    private final Ingredient[] original = new Ingredient[9];
    @Nullable
    private ItemStack result;
    private int amount;
    private int cookingTime;
    private int burnTime;
    private float experience;
    private boolean hidden;
    private boolean enabled;
    private String group;
    private boolean copyContents;
    @Nullable
    private String category;

    public RecipeEditorMenu(Player viewer, GardensRecipe recipe) {
        super(LAYOUT, "Recipe: " + recipe.getId(), viewer);
        this.id = recipe.getId();
        this.file = recipe.getFile();
        this.exists = RecipeManager.INSTANCE.getRecipes().containsKey(id);
        this.type = recipe.getType();
        this.amount = recipe.getAmount();
        this.cookingTime = type.isCooking() ? recipe.getCookingTime() : RecipeType.FURNACE.defaultCookingTime();
        this.burnTime = recipe.getBurnTime() > 0 ? recipe.getBurnTime() : 300;
        this.experience = recipe.getExperience();
        this.hidden = recipe.isHidden();
        this.enabled = recipe.isEnabled();
        this.group = recipe.getGroup();
        this.copyContents = recipe.isCopyContents();
        this.category = recipe.getCategory();
        load(recipe);
        buildElements();
    }

    private void load(GardensRecipe recipe) {
        switch (recipe.getType()) {
            case SHAPED -> {
                var shape = recipe.getShape();
                for (int y = 0; y < shape.size() && y < 3; y++) {
                    for (int x = 0; x < shape.get(y).length() && x < 3; x++) {
                        char c = shape.get(y).charAt(x);
                        if (c != ' ') setSlot(y * 3 + x, recipe.getKey().get(c), true);
                    }
                }
            }
            case SHAPELESS -> {
                var list = recipe.getIngredients();
                for (int i = 0; i < list.size() && i < 9; i++) setSlot(i, list.get(i), true);
            }
            default -> {
                if (recipe.getInput() != null) setSlot(4, recipe.getInput(), true);
            }
        }
        if (recipe.getResult() != null) {
            try {
                result = RecipeManager.INSTANCE.buildItem(recipe.getResult());
            } catch (Exception ignored) {
                // a result that no longer exists; the slot starts empty
            }
        }
    }

    private void setSlot(int i, @Nullable Ingredient ingredient, boolean fromRecipe) {
        ingredients[i] = ingredient;
        grid[i] = ingredient == null ? null : sample(ingredient);
        if (fromRecipe) original[i] = ingredient;
    }

    /** An item to show for an ingredient: the item itself, or the first item of its tag. */
    private static ItemStack sample(Ingredient ingredient) {
        try {
            var ref = ingredient.options().getFirst();
            if (ref.kind() != ItemRef.Kind.TAG) return RecipeManager.INSTANCE.buildItem(ref);
            var resolved = RecipeManager.INSTANCE.resolve(Ingredient.of(ref));
            if (!resolved.customIds().isEmpty()) return RecipeManager.INSTANCE.buildItem(ItemRef.custom(resolved.customIds().iterator().next()));
            return new ItemStack(resolved.materials().iterator().next());
        } catch (Exception e) {
            return new ItemStack(Material.BARRIER);
        }
    }

    private static ItemRef refOf(ItemStack stack) {
        var key = RecipeManager.keyOf(stack);
        return key.isCustom() ? ItemRef.custom(key.customId()) : ItemRef.material(key.material());
    }

    // ---- elements ----

    private void buildElements() {
        if (blankHiddenItem != null) gui.setFiller(blankHiddenItem);

        var gridGroup = new GuiElementGroup('g');
        for (int i = 0; i < 9; i++) {
            int slot = i;
            gridGroup.addElement(new DynamicGuiElement('g', () -> gridElement(slot)));
        }
        gui.addElement(gridGroup);
        gui.addElement(new DynamicGuiElement('r', this::resultElement));

        gui.addElement(dynamic('t', () -> button(Material.CRAFTING_TABLE, click -> {
            var types = RecipeType.values();
            type = types[Math.floorMod(type.ordinal() + (click.getType().isRightClick() ? -1 : 1), types.length)];
            if (type.isCooking() && cookingTime == RecipeType.FURNACE.defaultCookingTime()) cookingTime = type.defaultCookingTime();
            if (!GardensRecipe.categories(type).contains(category)) category = null;
            if (!type.isCrafting()) copyContents = false;
        }, "<yellow>Type: <white>" + type.configName(), "<gray>" + typeHelp(), "", "<dark_gray>Click: next · Right-click: previous")));

        gui.addElement(dynamic('a', () -> type == RecipeType.FUEL ? blank() : button(Material.FEATHER, click -> {
            int step = click.getType().isShiftClick() ? 8 : 1;
            amount = Math.clamp(amount + (click.getType().isRightClick() ? -step : step), 1, 99);
        }, "<yellow>Makes: <white>" + amount, "<dark_gray>Click: +1 · Right-click: -1 · Shift: ±8")));

        gui.addElement(dynamic('c', () -> {
            if (type.isCooking()) {
                return button(Material.CLOCK, click -> cookingTime = Math.max(1, cookingTime + step(click, 20, 100)),
                        "<yellow>Cooking time: <white>" + seconds(cookingTime), "<gray>Vanilla: furnace 10s, blast furnace and smoker 5s, campfire 30s",
                        "<dark_gray>Click: +1s · Right-click: -1s · Shift: ±5s");
            }
            if (type == RecipeType.FUEL) {
                return button(Material.BLAZE_POWDER, click -> burnTime = Math.max(1, burnTime + step(click, 20, 100)),
                        "<yellow>Burns for: <white>" + seconds(burnTime) + " <gray>(smelts " + String.format(Locale.ROOT, "%.1f", burnTime / 200.0) + " items)",
                        "<gray>A log burns for 15s", "<dark_gray>Click: +1s · Right-click: -1s · Shift: ±5s");
            }
            return blank();
        }));

        gui.addElement(dynamic('x', () -> !type.isCooking() ? blank() : button(Material.EXPERIENCE_BOTTLE, click -> {
            float step = click.getType().isShiftClick() ? 1f : 0.1f;
            experience = Math.max(0, Math.round((experience + (click.getType().isRightClick() ? -step : step)) * 10) / 10f);
        }, "<yellow>Experience: <white>" + experience, "<dark_gray>Click: +0.1 · Right-click: -0.1 · Shift: ±1")));

        gui.addElement(dynamic('k', () -> !type.isCrafting() ? blank() : button(copyContents ? Material.SHULKER_BOX : Material.BARREL,
                click -> copyContents = !copyContents,
                copyContents ? "<green>Keeps contents" : "<gray>Doesn't keep contents",
                "<gray>When on, the items inside an ingredient", "<gray>(a filled shulker box) go into the result", "<dark_gray>Click to toggle")));

        gui.addElement(dynamic('y', () -> {
            var tabs = GardensRecipe.categories(type);
            if (tabs.isEmpty()) return blank();
            return button(Material.BOOKSHELF, click -> {
                // null (misc) first, then each tab
                var options = new ArrayList<String>();
                if (tabs.contains("misc")) options.add(null);
                tabs.stream().filter(t -> !t.equals("misc")).forEach(options::add);
                int at = Math.max(0, options.indexOf(category));
                category = options.get(Math.floorMod(at + (click.getType().isRightClick() ? -1 : 1), options.size()));
            }, "<yellow>Recipe book tab: <white>" + (category != null ? category : tabs.contains("misc") ? "misc" : tabs.getFirst()),
                    "<gray>One of: " + String.join(", ", tabs), "<dark_gray>Click: next · Right-click: previous");
        }));

        gui.addElement(dynamic('h', () -> button(hidden ? Material.ENDER_EYE : Material.KNOWLEDGE_BOOK, click -> hidden = !hidden,
                hidden ? "<gold>Hidden from the recipe book" : "<green>Shown in the recipe book", "<dark_gray>Click to toggle")));

        gui.addElement(dynamic('p', () -> button(Material.NAME_TAG, click -> {
            if (click.getType().isRightClick()) {
                group = "";
                return;
            }
            ChatPrompt.INSTANCE.ask(viewer, "Type the recipe book group (recipes sharing one stack together):", answer -> {
                group = answer.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
                open();
            });
        }, "<yellow>Group: <white>" + (group.isEmpty() ? "none" : group), "<gray>Recipes in one group share a recipe book slot",
                "<dark_gray>Click: set in chat · Right-click: clear")));

        gui.addElement(dynamic('e', () -> button(enabled ? Material.LIME_DYE : Material.GRAY_DYE, click -> enabled = !enabled,
                enabled ? "<green>Enabled" : "<red>Disabled", "<gray>A disabled recipe stays in its file but can't be crafted", "<dark_gray>Click to toggle")));

        gui.addElement(new StaticGuiElement('b', new ItemStack(Material.ARROW), click -> {
            new RecipeListMenu(viewer).open();
            return true;
        }, "<yellow>Back to recipes", "<gray>Unsaved changes are lost"));

        gui.addElement(dynamic('d', () -> !exists ? blank() : button(Material.LAVA_BUCKET, click -> {
            if (!click.getType().isShiftClick()) return;
            try {
                RecipeManager.INSTANCE.delete(id);
                viewer.sendMessage(MM.deserialize("<green>Deleted recipe " + id));
                new RecipeListMenu(viewer).open();
            } catch (Exception e) {
                viewer.sendMessage(MM.deserialize("<red>Couldn't delete " + id + ": " + e.getMessage()));
            }
        }, "<red>Delete this recipe", "<gray>Shift-click to delete it from recipes/" + file)));

        gui.addElement(new StaticGuiElement('s', new ItemStack(Material.WRITABLE_BOOK), click -> {
            save();
            return true;
        }, "<green>Save", "<gray>Writes recipes/" + (file == null ? RecipeManager.DEFAULT_FILE : file), "<gray>and reloads every recipe"));
    }

    private static int step(GuiElement.Click click, int small, int big) {
        int step = click.getType().isShiftClick() ? big : small;
        return click.getType().isRightClick() ? -step : step;
    }

    private static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.1fs", ticks / 20.0) + " <gray>(" + ticks + " ticks)";
    }

    private String typeHelp() {
        return switch (type) {
            case SHAPED -> "Crafting, in this shape (may be mirrored)";
            case SHAPELESS -> "Crafting, in any slots";
            case FURNACE, BLASTING, SMOKING, CAMPFIRE -> "Cooks the item in the grid's middle";
            case STONECUTTING -> "Stonecutter: the item in the grid's middle";
            case FUEL -> "Makes the item in the grid's middle burn in furnaces";
        };
    }

    private interface ClickHandler {
        void handle(GuiElement.Click click);
    }

    private DynamicGuiElement dynamic(char c, java.util.function.Supplier<GuiElement> supplier) {
        return new DynamicGuiElement(c, supplier);
    }

    private GuiElement button(Material icon, ClickHandler handler, String... text) {
        return new StaticGuiElement(' ', new ItemStack(icon), click -> {
            handler.handle(click);
            gui.draw();
            return true;
        }, text);
    }

    private GuiElement blank() {
        return new StaticGuiElement(' ', blankHiddenItem != null ? blankHiddenItem : new ItemStack(Material.AIR), " ");
    }

    private GuiElement gridElement(int i) {
        boolean usedSlot = !type.hasInput() || i == 4;
        GuiElement.Action action = click -> {
            var cursor = click.getCursor();
            if (!ItemUtil.isEmpty(cursor)) {
                var copy = cursor.clone();
                copy.setAmount(1);
                grid[i] = copy;
                ingredients[i] = Ingredient.of(refOf(copy));
                original[i] = null;
            } else if (grid[i] != null && click.getType().isRightClick()) {
                cycle(i, click.getType().isShiftClick() ? -1 : 1);
            } else {
                setSlot(i, null, false);
            }
            gui.draw();
            return true;
        };
        if (grid[i] == null) {
            var empty = blankHiddenItem != null ? blankHiddenItem : new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            return new StaticGuiElement(' ', empty, action, usedSlot ? "<gray>Empty" : "<dark_gray>Unused for " + type.configName(),
                    "<dark_gray>Click with an item to put a copy here");
        }
        var shown = grid[i].clone();
        var lore = new ArrayList<net.kyori.adventure.text.Component>();
        lore.add(MM.deserialize("<!italic><gray>Counts as: <white>" + describe(ingredients[i])));
        if (!usedSlot) lore.add(MM.deserialize("<!italic><red>Ignored: " + type.configName() + " only uses the middle slot"));
        lore.add(MM.deserialize("<!italic><dark_gray>Right-click: any item of a tag · Shift-right: back"));
        lore.add(MM.deserialize("<!italic><dark_gray>Click empty-handed: clear · With an item: replace"));
        shown.lore(lore);
        return new StaticGuiElement(' ', shown, action);
    }

    private static String describe(Ingredient ingredient) {
        var ref = ingredient.options().getFirst();
        if (ingredient.options().size() > 1) return "any of " + ingredient;
        return switch (ref.kind()) {
            case TAG -> "any " + ref;
            case CUSTOM, MATERIAL -> "exactly " + ref;
        };
    }

    /** Steps the slot through: the exact item, what it was loaded with, then each tag holding the item. */
    private void cycle(int i, int direction) {
        var exact = Ingredient.of(refOf(grid[i]));
        var options = new ArrayList<Ingredient>(List.of(exact));
        if (original[i] != null && !options.contains(original[i])) options.add(original[i]);
        var key = RecipeManager.keyOf(grid[i]);
        var vanillaTags = key.isCustom() ? RecipeManager.INSTANCE.vanillaTagsOf(key.customId()) : RecipeManager.INSTANCE.vanillaTagsOf(key.material());
        vanillaTags.forEach(tag -> options.add(Ingredient.of(ItemRef.tag(tag.toString()))));
        RecipeManager.INSTANCE.customTagsOf(key).forEach(tag -> options.add(Ingredient.of(ItemRef.tag(ItemRef.CUSTOM_NAMESPACE + ":" + tag))));
        int current = Math.max(0, options.indexOf(ingredients[i]));
        ingredients[i] = options.get(Math.floorMod(current + direction, options.size()));
        if (options.size() == 1) {
            viewer.sendMessage(MM.deserialize("<gray>That item isn't in any tag. Add your own in recipe_tags.yml."));
        }
    }

    private GuiElement resultElement() {
        GuiElement.Action action = click -> {
            var cursor = click.getCursor();
            if (!ItemUtil.isEmpty(cursor)) {
                result = cursor.clone();
                result.setAmount(1);
                if (cursor.getAmount() > 1) amount = cursor.getAmount();
            } else {
                result = null;
            }
            gui.draw();
            return true;
        };
        if (type == RecipeType.FUEL) {
            return new StaticGuiElement(' ', new ItemStack(Material.BLAZE_POWDER), action, "<gray>Fuel has no result");
        }
        if (result == null) {
            return new StaticGuiElement(' ', new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE), action,
                    "<yellow>Result", "<gray>Click with the item it should make");
        }
        var shown = result.clone();
        shown.setAmount(amount);
        var lore = new ArrayList<net.kyori.adventure.text.Component>();
        lore.add(MM.deserialize("<!italic><gray>Makes " + amount + " × <white>" + refOf(result)));
        lore.add(MM.deserialize("<!italic><dark_gray>Click empty-handed: clear · With an item: replace"));
        shown.lore(lore);
        return new StaticGuiElement(' ', shown, action);
    }

    // ---- saving ----

    private GardensRecipe build() {
        var recipe = new GardensRecipe(id, type);
        recipe.setFile(file);
        recipe.setGroup(group);
        recipe.setEnabled(enabled);
        recipe.setHidden(hidden);
        recipe.setCopyContents(copyContents);
        recipe.setCategory(category);
        switch (type) {
            case SHAPED -> recipe.setGrid(ingredients.clone(), 3);
            case SHAPELESS -> {
                for (var ingredient : ingredients) if (ingredient != null) recipe.getIngredients().add(ingredient);
            }
            default -> {
                if (ingredients[4] == null) throw new IllegalArgumentException("put the input in the grid's middle slot");
                recipe.setInput(ingredients[4]);
            }
        }
        if (type == RecipeType.FUEL) {
            recipe.setBurnTime(burnTime);
        } else {
            if (result == null) throw new IllegalArgumentException("set a result");
            recipe.setResult(refOf(result));
            recipe.setAmount(amount);
        }
        if (type.isCooking()) {
            recipe.setCookingTime(cookingTime);
            recipe.setExperience(experience);
        }
        recipe.validate();
        return recipe;
    }

    private void save() {
        GardensRecipe recipe;
        try {
            recipe = build();
        } catch (IllegalArgumentException e) {
            viewer.sendMessage(MM.deserialize("<red>Can't save yet: " + e.getMessage()));
            return;
        }
        try {
            RecipeManager.INSTANCE.save(recipe);
        } catch (Exception e) {
            viewer.sendMessage(MM.deserialize("<red>Couldn't save " + id + ": " + e.getMessage()));
            return;
        }
        if (RecipeManager.INSTANCE.getCompiled(id) == null && enabled && type != RecipeType.FUEL) {
            viewer.sendMessage(MM.deserialize("<yellow>Saved " + id + ", but it didn't load; see the console"));
        } else {
            viewer.sendMessage(MM.deserialize("<green>Saved " + id + " to recipes/" + recipe.getFile()));
        }
        new RecipeEditorMenu(viewer, recipe).open();
    }
}
