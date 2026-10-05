package com.gardensmc.gardensfurniture.menu;

import com.gardensmc.gardensfurniture.recipe.*;
import com.gardensmc.gardensfurniture.util.ItemUtil;
import de.themoep.inventorygui.DynamicGuiElement;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiStorageElement;
import de.themoep.inventorygui.StaticGuiElement;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Builds or edits one recipe. The grid and result work like any inventory: put items in, pick them up,
 * move them around. Tag mode turns the grid into buttons instead: clicking an item cycles what that
 * slot accepts, the exact item or any item in one of its tags ("any plank").
 *
 * <p>Items you put in come back when the editor closes. The ones the editor makes to show a saved
 * recipe are marked and never leave it: dragged into your inventory, they're taken back on close.
 */
public class RecipeEditorMenu extends Menu {

    private static final String[] LAYOUT = new String[]{
            "  m      ",
            " ggg  fff",
            " ggg >frf",
            " ggg  fff",
            "t a c x k",
            "bhpey  ds"
    };
    private static final MiniMessage MM = MiniMessage.miniMessage();
    // background and the frame around the result; the grid and result themselves are plain slots
    private static final Material BACKGROUND = Material.GRAY_STAINED_GLASS_PANE;
    private static final Material RESULT_FRAME = Material.LIME_STAINED_GLASS_PANE;
    private static final int RESULT_SLOT = 9;
    // on items the editor made itself, which never leave it
    public static final NamespacedKey EDITOR_ITEM = new NamespacedKey("gardensfurniture", "recipe_editor_item");
    // what a grid item counts as when it isn't just itself ("#minecraft:planks"), stripped when it leaves
    private static final NamespacedKey COUNTS_AS = new NamespacedKey("gardensfurniture", "recipe_counts_as");

    private final String id;
    @Nullable
    private final String file;
    private final boolean exists;

    private RecipeType type;
    // grid slots 0-8, then the result; the rest unused (inventories come in rows of 9)
    private final Inventory storage = Bukkit.createInventory(null, 18);
    private boolean tagMode;
    // set while the player types in chat with the editor closed, so closing doesn't hand items back
    private boolean prompting;
    @Nullable
    private ItemStack lastResult;
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
        // the editor's own items; kept apart from anything the player puts in
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
                var result = RecipeManager.INSTANCE.buildItem(recipe.getResult());
                result.setAmount(Math.min(recipe.getAmount(), result.getMaxStackSize()));
                result.editPersistentDataContainer(pdc -> pdc.set(EDITOR_ITEM, PersistentDataType.BOOLEAN, true));
                storage.setItem(RESULT_SLOT, result);
                lastResult = result.clone();
            } catch (Exception ignored) {
                // a result that no longer exists; the slot starts empty
            }
        }
    }

    private void setSlot(int i, Ingredient ingredient, boolean fromRecipe) {
        var stack = sample(ingredient);
        stack.editPersistentDataContainer(pdc -> {
            pdc.set(EDITOR_ITEM, PersistentDataType.BOOLEAN, true);
            if (!ingredient.equals(Ingredient.of(refOf(stack)))) pdc.set(COUNTS_AS, PersistentDataType.STRING, encode(ingredient));
        });
        storage.setItem(i, stack);
    }

    /** What the grid item in slot i counts as, or null for an empty slot. */
    @Nullable
    private Ingredient ingredientAt(int i) {
        var stack = storage.getItem(i);
        if (ItemUtil.isEmpty(stack)) return null;
        var countsAs = stack.getPersistentDataContainer().get(COUNTS_AS, PersistentDataType.STRING);
        if (countsAs != null) {
            try {
                return decode(countsAs);
            } catch (Exception ignored) {
                // unreadable: the item itself
            }
        }
        return Ingredient.of(refOf(stack));
    }

    private static String encode(Ingredient ingredient) {
        return String.join(" ", ingredient.options().stream().map(ItemRef::toString).toList());
    }

    private static Ingredient decode(String encoded) {
        return new Ingredient(Arrays.stream(encoded.split(" ")).map(ItemRef::parse).toList());
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
        gui.setFiller(pane(BACKGROUND));
        gui.addElement(new StaticGuiElement('f', pane(RESULT_FRAME), " "));
        gui.addElement(new StaticGuiElement('>', new ItemStack(Material.ARROW), "<yellow>How to use",
                "<gray>Put the ingredients in the grid and what it",
                "<gray>makes on the right, like any inventory.",
                "<gray>Your own items come back when you close this.",
                "<gray>For \"any plank\" and such, turn on tag mode above."));

        var gridGroup = new GuiElementGroup('g');
        for (int i = 0; i < 9; i++) {
            int slot = i;
            gridGroup.addElement(new DynamicGuiElement('g', () -> tagMode ? tagElement(slot) : storageElement(slot)));
        }
        gui.addElement(gridGroup);
        gui.addElement(new DynamicGuiElement('r', this::resultElement));

        gui.addElement(dynamic('m', () -> button(tagMode ? Material.NAME_TAG : Material.PAPER, click -> tagMode = !tagMode,
                tagMode ? "<gold>Tag mode: on" : "<yellow>Tag mode: off",
                tagMode ? "<gray>Click a grid item to cycle what it counts as:" : "<gray>Turn on to choose what each grid item counts as:",
                "<gray>the exact item, any plank, any log…",
                tagMode ? "<gray>Turn off to move items again." : "<gray>Items can't be moved while it's on.",
                "<dark_gray>Click to toggle")));

        gui.setCloseAction(close -> {
            if (!prompting) returnItems();
            return false;
        });

        gui.addElement(dynamic('t', () -> button(Material.CRAFTING_TABLE, click -> {
            var types = RecipeType.values();
            type = types[Math.floorMod(type.ordinal() + (click.getType().isRightClick() ? -1 : 1), types.length)];
            if (type.isCooking() && cookingTime == RecipeType.FURNACE.defaultCookingTime()) cookingTime = type.defaultCookingTime();
            // List.of(...).contains(null) throws, and no tab set is fine for every type
            if (category != null && !GardensRecipe.categories(type).contains(category)) category = null;
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
            prompting = true;
            ChatPrompt.INSTANCE.ask(viewer, "Type the recipe book group (recipes sharing one stack together):", answer -> {
                prompting = false;
                group = answer.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
                open();
            }, () -> {
                prompting = false;
                open();
            }, () -> {
                prompting = false;
                returnItems();
            });
        }, "<yellow>Group: <white>" + (group.isEmpty() ? "none" : group), "<gray>Recipes in one group share a recipe book slot",
                "<dark_gray>Click: set in chat · Right-click: clear")));

        gui.addElement(dynamic('e', () -> button(enabled ? Material.LIME_DYE : Material.GRAY_DYE, click -> enabled = !enabled,
                enabled ? "<green>Enabled" : "<red>Disabled", "<gray>A disabled recipe stays in its file but can't be crafted", "<dark_gray>Click to toggle")));

        gui.addElement(new StaticGuiElement('b', new ItemStack(Material.ARROW), click -> {
            returnItems();
            new RecipeListMenu(viewer).open();
            return true;
        }, "<yellow>Back to recipes", "<gray>Unsaved changes are lost"));

        gui.addElement(dynamic('d', () -> !exists ? blank() : button(Material.LAVA_BUCKET, click -> {
            if (!click.getType().isShiftClick()) return;
            try {
                RecipeManager.INSTANCE.delete(id);
                viewer.sendMessage(MM.deserialize("<green>Deleted recipe " + id));
                returnItems();
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
            case FURNACE, BLASTING, SMOKING, CAMPFIRE -> "Cooks the one item in the grid";
            case STONECUTTING -> "Stonecutter: the one item in the grid";
            case FUEL -> "Makes the one item in the grid burn in furnaces";
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

    // a glass pane with no tooltip
    private static ItemStack pane(Material material) {
        var pane = new ItemStack(material);
        pane.editMeta(meta -> meta.setHideTooltip(true));
        return pane;
    }

    private GuiElement blank() {
        return new StaticGuiElement(' ', pane(BACKGROUND), " ");
    }

    // a plain inventory slot
    private GuiElement storageElement(int i) {
        return new GuiStorageElement(' ', storage, i, this::onStorageChanged, info -> true);
    }

    private void onStorageChanged() {
        // a new result sets how many it makes; the amount button can still change it after
        var result = storage.getItem(RESULT_SLOT);
        if (!ItemUtil.isEmpty(result) && (lastResult == null || !lastResult.isSimilar(result) || lastResult.getAmount() != result.getAmount())) {
            amount = result.getAmount();
        }
        lastResult = ItemUtil.isEmpty(result) ? null : result.clone();
        // the GUI redraws mid-click otherwise, losing the cursor
        Bukkit.getScheduler().runTask(com.gardensmc.gardensfurniture.GardensFurniture.plugin, () -> gui.draw());
    }

    // tag mode: the grid item with what it counts as; clicking cycles it
    private GuiElement tagElement(int i) {
        var stack = storage.getItem(i);
        if (ItemUtil.isEmpty(stack)) return new StaticGuiElement(' ', null, click -> true);
        var shown = stack.clone();
        var lore = new ArrayList<net.kyori.adventure.text.Component>();
        lore.add(MM.deserialize("<!italic><gray>Counts as: <white>" + describe(ingredientAt(i))));
        if (type.hasInput() && filledSlots() > 1) {
            lore.add(MM.deserialize("<!italic><red>" + type.configName() + " takes one item: leave just one in the grid"));
        }
        lore.add(MM.deserialize("<!italic><dark_gray>Click: next · Right-click: previous"));
        shown.lore(lore);
        return new StaticGuiElement(' ', shown, click -> {
            cycle(i, click.getType().isRightClick() ? -1 : 1);
            gui.draw();
            return true;
        });
    }

    private int filledSlots() {
        int filled = 0;
        for (int i = 0; i < 9; i++) if (!ItemUtil.isEmpty(storage.getItem(i))) filled++;
        return filled;
    }

    private static String describe(Ingredient ingredient) {
        var ref = ingredient.options().getFirst();
        if (ingredient.options().size() > 1) return "any of " + ingredient;
        return switch (ref.kind()) {
            case TAG -> "any " + ref;
            case CUSTOM, MATERIAL -> "exactly " + ref;
        };
    }

    /** Steps the slot through: the exact item, what it counts as now, then each tag holding the item. */
    private void cycle(int i, int direction) {
        var stack = storage.getItem(i);
        if (ItemUtil.isEmpty(stack)) return;
        var exact = Ingredient.of(refOf(stack));
        var current = ingredientAt(i);
        var options = new ArrayList<Ingredient>(List.of(exact));
        if (!options.contains(current)) options.add(current);
        var key = RecipeManager.keyOf(stack);
        var vanillaTags = key.isCustom() ? RecipeManager.INSTANCE.vanillaTagsOf(key.customId()) : RecipeManager.INSTANCE.vanillaTagsOf(key.material());
        vanillaTags.forEach(tag -> options.add(Ingredient.of(ItemRef.tag(tag.toString()))));
        RecipeManager.INSTANCE.customTagsOf(key).forEach(tag -> options.add(Ingredient.of(ItemRef.tag(ItemRef.CUSTOM_NAMESPACE + ":" + tag))));
        var next = options.get(Math.floorMod(Math.max(0, options.indexOf(current)) + direction, options.size()));
        stack.editPersistentDataContainer(pdc -> {
            if (next.equals(exact)) pdc.remove(COUNTS_AS);
            else pdc.set(COUNTS_AS, PersistentDataType.STRING, encode(next));
        });
        storage.setItem(i, stack);
        if (options.size() == 1) {
            viewer.sendMessage(MM.deserialize("<gray>That item isn't in any tag. Add your own in recipe_tags.yml."));
        }
    }

    private GuiElement resultElement() {
        if (type == RecipeType.FUEL) {
            return new StaticGuiElement(' ', new ItemStack(Material.BLAZE_POWDER), click -> true, "<gray>Fuel has no result");
        }
        if (tagMode) {
            var result = storage.getItem(RESULT_SLOT);
            return new StaticGuiElement(' ', ItemUtil.isEmpty(result) ? null : result.clone(), click -> true);
        }
        return storageElement(RESULT_SLOT);
    }

    /**
     * Gives back what the player put in, keeps the editor's own items: from the grid, and any the player
     * dragged into their inventory or holds on the cursor.
     */
    private void returnItems() {
        var back = new ArrayList<ItemStack>();
        for (int i = 0; i < storage.getSize(); i++) {
            var stack = storage.getItem(i);
            storage.setItem(i, null);
            if (ItemUtil.isEmpty(stack) || isEditorItem(stack)) continue;
            back.add(stripped(stack));
        }
        var inventory = viewer.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            var stack = inventory.getItem(i);
            if (ItemUtil.isEmpty(stack)) continue;
            if (isEditorItem(stack)) inventory.setItem(i, null);
            else if (stack.getPersistentDataContainer().has(COUNTS_AS)) inventory.setItem(i, stripped(stack));
        }
        var cursor = viewer.getItemOnCursor();
        if (!ItemUtil.isEmpty(cursor)) {
            viewer.setItemOnCursor(isEditorItem(cursor) ? null : stripped(cursor));
        }
        inventory.addItem(back.toArray(ItemStack[]::new)).values()
                .forEach(leftover -> viewer.getWorld().dropItemNaturally(viewer.getLocation(), leftover));
    }

    private static boolean isEditorItem(ItemStack stack) {
        return stack.getPersistentDataContainer().has(EDITOR_ITEM);
    }

    private static ItemStack stripped(ItemStack stack) {
        if (!stack.getPersistentDataContainer().has(COUNTS_AS)) return stack;
        var copy = stack.clone();
        copy.editPersistentDataContainer(pdc -> pdc.remove(COUNTS_AS));
        return copy;
    }

    // ---- saving ----

    private Ingredient[] ingredients() {
        var grid = new Ingredient[9];
        for (int i = 0; i < 9; i++) grid[i] = ingredientAt(i);
        return grid;
    }

    private GardensRecipe build() {
        var recipe = new GardensRecipe(id, type);
        recipe.setFile(file);
        recipe.setGroup(group);
        recipe.setEnabled(enabled);
        recipe.setHidden(hidden);
        recipe.setCopyContents(copyContents);
        recipe.setCategory(category);
        switch (type) {
            case SHAPED -> recipe.setGrid(ingredients(), 3);
            case SHAPELESS -> {
                for (var ingredient : ingredients()) if (ingredient != null) recipe.getIngredients().add(ingredient);
            }
            default -> {
                Ingredient input = null;
                for (var ingredient : ingredients()) {
                    if (ingredient == null) continue;
                    if (input != null) throw new IllegalArgumentException(type.configName() + " takes one item: leave just one in the grid");
                    input = ingredient;
                }
                if (input == null) throw new IllegalArgumentException("put the item it takes in the grid");
                recipe.setInput(input);
            }
        }
        if (type == RecipeType.FUEL) {
            recipe.setBurnTime(burnTime);
        } else {
            var result = storage.getItem(RESULT_SLOT);
            if (ItemUtil.isEmpty(result)) throw new IllegalArgumentException("put what it makes in the result slot");
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
        returnItems();
        new RecipeEditorMenu(viewer, recipe).open();
    }
}
