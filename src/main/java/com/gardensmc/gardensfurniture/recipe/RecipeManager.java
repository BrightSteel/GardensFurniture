package com.gardensmc.gardensfurniture.recipe;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.register.ItemRegistry;
import com.gardensmc.gardensfurniture.util.ItemUtil;
import com.gardensmc.gardensfurniture.util.SpecialItems;
import de.tr7zw.nbtapi.NBT;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.recipe.CookingBookCategory;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Loads recipes/*.yml and recipe_tags.yml and registers them as real server recipes, so the recipe
 * book shows them and the server consumes ingredients itself.
 *
 * <p>Ingredients that are GardensFurniture items are matched by their id, not by the whole stack: a
 * quilt crafted months ago, with an older name or lore, still counts as a quilt. The server can only
 * compare whole stacks, so a recipe with custom ingredients is registered twice:
 * <ul>
 *     <li>the book recipe, which lists the current items and is what the recipe book shows</li>
 *     <li>a loose copy that accepts the items' base material (paper, ...), never shown in the book,
 *     so older copies of the items still reach the craft event</li>
 * </ul>
 * {@link RecipeListener} then checks the ids and sets (or clears) the result. Setting a result for a
 * grid no server recipe matches would hand the ingredients back when it's taken, so there is always
 * a server recipe underneath.
 */
public class RecipeManager {

    public static final RecipeManager INSTANCE = new RecipeManager();
    public static final String DEFAULT_FILE = "custom.yml";
    private static final String LOOSE_PREFIX = "any/";

    /** A loaded, enabled recipe with its tags expanded. */
    public record Compiled(GardensRecipe recipe, List<String> shape, Map<Character, ResolvedIngredient> key,
                           List<ResolvedIngredient> ingredients, @Nullable ResolvedIngredient input,
                           NamespacedKey bookKey, @Nullable NamespacedKey looseKey) {

        boolean matches(ItemKey[] grid, int gridWidth) {
            return switch (recipe.getType()) {
                case SHAPED -> RecipeMatcher.matchesShaped(shape, key, grid, gridWidth);
                case SHAPELESS -> RecipeMatcher.matchesShapeless(ingredients, grid);
                default -> false;
            };
        }
    }

    // every recipe in the files by id, enabled or not, for the editor
    @Getter
    private volatile Map<String, GardensRecipe> recipes = Map.of();
    private volatile List<Compiled> crafting = List.of();
    private volatile Map<String, Compiled> compiledById = Map.of();
    // fuel burn time in ticks, by GardensFurniture id or vanilla material
    private volatile Map<String, Integer> customFuels = Map.of();
    private volatile Map<Material, Integer> vanillaFuels = Map.of();
    private volatile Map<String, List<ItemRef>> customTags = Map.of();
    private final Set<NamespacedKey> registeredKeys = new HashSet<>();
    private final Set<NamespacedKey> bookKeys = new HashSet<>();
    private final Set<NamespacedKey> looseKeys = new HashSet<>();

    private RecipeManager() {
    }

    public File recipesDir() {
        return new File(GardensFurniture.plugin.getDataFolder(), "recipes");
    }

    private Logger log() {
        return GardensFurniture.plugin.getLogger();
    }

    /** (Re)reads every recipe file and replaces the server recipes with them. Items must be loaded first. */
    public void reload() {
        loadTags();
        var loaded = new LinkedHashMap<String, GardensRecipe>();
        int failed = 0;
        var dir = recipesDir();
        dir.mkdirs();
        writeDefaultBackpackRecipes(dir);
        for (var file : recipeFiles(dir)) {
            var relative = dir.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
            var config = YamlConfiguration.loadConfiguration(file);
            for (var id : config.getKeys(false)) {
                var section = config.getConfigurationSection(id);
                if (section == null) continue;
                try {
                    var recipe = GardensRecipe.fromConfig(id, section);
                    recipe.setFile(relative);
                    var existing = loaded.putIfAbsent(id, recipe);
                    if (existing != null) {
                        log().warning("Recipe '%s' in recipes/%s is also in recipes/%s; ignoring the second".formatted(id, relative, existing.getFile()));
                    }
                } catch (Exception e) {
                    failed++;
                    log().severe("Skipping recipe '%s' in recipes/%s: %s".formatted(id, relative, e.getMessage()));
                }
            }
        }
        this.recipes = Collections.unmodifiableMap(loaded);
        failed += register(loaded.values());
        log().info("Loaded %d recipe(s)%s".formatted(loaded.size() - failed, failed > 0 ? ", %d failed (see above)".formatted(failed) : ""));
    }

    /**
     * recipes/backpacks.yml, written when it's missing: leather around a shulker box makes the backpack
     * of the box's color (the plain one when that color has none), carrying the box's items over.
     */
    private void writeDefaultBackpackRecipes(File dir) {
        var file = new File(dir, "backpacks.yml");
        var plain = SpecialItems.BACKPACK_PREFIX;
        if (file.exists() || GardensFurniture.itemRegistry.getItem(plain) == null) return;
        var config = new YamlConfiguration();
        config.options().setHeader(List.of(
                "Backpacks: leather around a shulker box, keeping what's inside the box.",
                "Edit these like any recipe. Turn one off with enabled: false; delete this whole",
                "file and it's written again on the next start or reload."));
        var shulkers = new LinkedHashMap<String, Material>();
        shulkers.put("", Material.SHULKER_BOX);
        for (var color : DyeColor.values()) {
            var name = color.name().toLowerCase(Locale.ROOT);
            var material = Material.matchMaterial(name + "_shulker_box");
            if (material != null) shulkers.put("_" + name, material);
        }
        shulkers.forEach((suffix, shulker) -> {
            var result = GardensFurniture.itemRegistry.getItem(plain + suffix) != null ? plain + suffix : plain;
            var recipe = new GardensRecipe(plain + suffix, RecipeType.SHAPED);
            recipe.setGroup(plain);
            recipe.setShape(new ArrayList<>(List.of(" L ", "LSL", " L ")));
            recipe.getKey().put('L', Ingredient.of(ItemRef.material(Material.LEATHER)));
            recipe.getKey().put('S', Ingredient.of(ItemRef.material(shulker)));
            recipe.setResult(ItemRef.custom(result));
            recipe.setCopyContents(true);
            recipe.setCategory("equipment");
            recipe.writeTo(config.createSection(recipe.getId()));
        });
        try {
            config.save(file);
            log().info("Wrote the default backpack recipes to recipes/backpacks.yml");
        } catch (IOException e) {
            log().severe("Couldn't write recipes/backpacks.yml: " + e.getMessage());
        }
    }

    private static List<File> recipeFiles(File dir) {
        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            return walk.filter(p -> p.toString().endsWith(".yml")).map(Path::toFile).sorted().toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadTags() {
        var file = new File(GardensFurniture.plugin.getDataFolder(), "recipe_tags.yml");
        if (!file.exists()) {
            GardensFurniture.plugin.saveResource("recipe_tags.yml", false);
        }
        var config = YamlConfiguration.loadConfiguration(file);
        var tags = new HashMap<String, List<ItemRef>>();
        for (var name : config.getKeys(false)) {
            try {
                tags.put(name, Ingredient.parse(config.get(name)).options());
            } catch (Exception e) {
                log().severe("Skipping tag '%s' in recipe_tags.yml: %s".formatted(name, e.getMessage()));
            }
        }
        this.customTags = tags;
    }

    private int register(Collection<GardensRecipe> all) {
        // out with the old
        for (var key : registeredKeys) {
            Bukkit.removeRecipe(key, false);
        }
        registeredKeys.clear();
        bookKeys.clear();
        looseKeys.clear();

        var world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().getFirst();
        var compiledList = new ArrayList<Compiled>();
        var byId = new HashMap<String, Compiled>();
        var fuelsCustom = new HashMap<String, Integer>();
        var fuelsVanilla = new HashMap<Material, Integer>();
        var toAdd = new ArrayList<Recipe>();
        int failed = 0;
        for (var recipe : all) {
            if (!recipe.isEnabled()) continue;
            try {
                if (recipe.getType() == RecipeType.FUEL) {
                    var fuel = resolve(Objects.requireNonNull(recipe.getInput()));
                    fuel.customIds().forEach(id -> fuelsCustom.put(id, recipe.getBurnTime()));
                    fuel.materials().forEach(m -> fuelsVanilla.put(m, recipe.getBurnTime()));
                    continue;
                }
                var compiled = compile(recipe);
                var result = buildResult(recipe);
                toAdd.add(toServerRecipe(compiled, compiled.bookKey(), result, false));
                if (compiled.looseKey() != null) {
                    var loose = toServerRecipe(compiled, compiled.looseKey(), result, true);
                    var clash = world == null ? null : vanillaClash(compiled, world);
                    if (clash != null) {
                        // the server would pick one of the two at random-ish; keep vanilla's working
                        log().warning(("Recipe '%s': with its items' base materials the grid is also vanilla's %s, so only "
                                + "up-to-date copies of its GardensFurniture items will craft it").formatted(recipe.getId(), clash));
                        compiled = new Compiled(compiled.recipe(), compiled.shape(), compiled.key(), compiled.ingredients(), compiled.input(), compiled.bookKey(), null);
                    } else {
                        toAdd.add(loose);
                    }
                }
                if (recipe.getType().isCrafting()) compiledList.add(compiled);
                byId.put(recipe.getId(), compiled);
            } catch (Exception e) {
                failed++;
                log().severe("Skipping recipe '%s' in recipes/%s: %s".formatted(recipe.getId(), recipe.getFile(), e.getMessage()));
            }
        }

        for (var recipe : toAdd) {
            var key = ((Keyed) recipe).getKey();
            // still there after a plugin reload, when registeredKeys started out empty
            Bukkit.removeRecipe(key, false);
            if (Bukkit.addRecipe(recipe, false)) {
                registeredKeys.add(key);
                if (key.getKey().startsWith(LOOSE_PREFIX)) looseKeys.add(key);
            } else {
                log().warning("Server refused recipe " + key);
            }
        }
        for (var compiled : byId.values()) {
            if (!compiled.recipe().isHidden() && registeredKeys.contains(compiled.bookKey())) {
                bookKeys.add(compiled.bookKey());
            }
        }
        this.crafting = List.copyOf(compiledList);
        this.compiledById = Map.copyOf(byId);
        this.customFuels = Map.copyOf(fuelsCustom);
        this.vanillaFuels = Map.copyOf(fuelsVanilla);

        Bukkit.updateRecipes();
        Bukkit.getOnlinePlayers().forEach(this::updateRecipeBook);
        return failed;
    }

    private Compiled compile(GardensRecipe recipe) {
        var id = recipe.getId();
        var bookKey = new NamespacedKey(GardensFurniture.plugin, id);
        List<String> shape = List.of();
        var key = new LinkedHashMap<Character, ResolvedIngredient>();
        var ingredients = new ArrayList<ResolvedIngredient>();
        ResolvedIngredient input = null;
        switch (recipe.getType()) {
            case SHAPED -> {
                shape = RecipeMatcher.trim(recipe.getShape());
                for (var row : shape) {
                    for (char c : row.toCharArray()) {
                        if (c != ' ' && !key.containsKey(c)) key.put(c, resolve(recipe.getKey().get(c)));
                    }
                }
            }
            case SHAPELESS -> recipe.getIngredients().forEach(i -> ingredients.add(resolve(i)));
            default -> input = resolve(Objects.requireNonNull(recipe.getInput()));
        }
        var all = new ArrayList<ResolvedIngredient>(key.values());
        all.addAll(ingredients);
        if (input != null) all.add(input);
        // the furnace has no event to check ids in before it starts, so only crafting gets a loose copy
        boolean needsLoose = recipe.getType().isCrafting() && all.stream().anyMatch(i -> !i.customIds().isEmpty());
        var looseKey = needsLoose ? new NamespacedKey(GardensFurniture.plugin, LOOSE_PREFIX + id) : null;
        return new Compiled(recipe, shape, key, ingredients, input, bookKey, looseKey);
    }

    /** Expands tags; throws when nothing at all fits, which is always a typo. */
    public ResolvedIngredient resolve(Ingredient ingredient) {
        var materials = new LinkedHashSet<Material>();
        var customIds = new LinkedHashSet<String>();
        for (var ref : ingredient.options()) {
            resolveInto(ref, materials, customIds, new HashSet<>());
        }
        var resolved = new ResolvedIngredient(materials, customIds);
        if (resolved.isEmpty()) throw new IllegalArgumentException("nothing fits " + ingredient);
        return resolved;
    }

    private void resolveInto(ItemRef ref, Set<Material> materials, Set<String> customIds, Set<String> seenTags) {
        switch (ref.kind()) {
            case MATERIAL -> {
                var material = ref.toMaterial();
                if (material == null) throw new IllegalArgumentException("unknown item " + ref);
                materials.add(material);
            }
            case CUSTOM -> {
                if (GardensFurniture.itemRegistry.getItem(ref.value()) == null) {
                    throw new IllegalArgumentException("no GardensFurniture item " + ref);
                }
                customIds.add(ref.value());
            }
            case TAG -> {
                if (!seenTags.add(ref.value())) return; // a tag that includes itself
                var key = NamespacedKey.fromString(ref.value());
                if (key == null) throw new IllegalArgumentException("bad tag " + ref);
                if (key.getNamespace().equals(ItemRef.CUSTOM_NAMESPACE)) {
                    var tag = customTags.get(key.getKey());
                    if (tag == null) throw new IllegalArgumentException("no tag " + ref + " in recipe_tags.yml");
                    for (var inner : tag) resolveInto(inner, materials, customIds, seenTags);
                } else {
                    var tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, key, Material.class);
                    if (tag == null) throw new IllegalArgumentException("unknown item tag " + ref);
                    tag.getValues().stream().filter(m -> m.isItem() && !m.isAir()).forEach(materials::add);
                }
            }
        }
    }

    public ItemStack buildResult(GardensRecipe recipe) {
        var stack = buildItem(Objects.requireNonNull(recipe.getResult(), "missing result"));
        stack.setAmount(recipe.getAmount());
        return stack;
    }

    /** A stack of one of the referenced item; tags aren't allowed here. */
    public ItemStack buildItem(ItemRef ref) {
        return switch (ref.kind()) {
            case MATERIAL -> {
                var material = ref.toMaterial();
                if (material == null) throw new IllegalArgumentException("unknown item " + ref);
                yield new ItemStack(material);
            }
            case CUSTOM -> {
                var stack = GardensFurniture.itemRegistry.buildItem(ref.value());
                if (stack == null) throw new IllegalArgumentException("no GardensFurniture item " + ref);
                yield stack;
            }
            case TAG -> throw new IllegalArgumentException("a tag can't be built: " + ref);
        };
    }

    private Recipe toServerRecipe(Compiled compiled, NamespacedKey key, ItemStack result, boolean loose) {
        var recipe = compiled.recipe();
        var group = recipe.getGroup();
        return switch (recipe.getType()) {
            case SHAPED -> {
                var shaped = new ShapedRecipe(key, result).shape(compiled.shape().toArray(String[]::new));
                compiled.key().forEach((c, ingredient) -> shaped.setIngredient(c, choice(ingredient, loose)));
                shaped.setGroup(group);
                shaped.setCategory(craftingCategory(recipe));
                yield shaped;
            }
            case SHAPELESS -> {
                var shapeless = new ShapelessRecipe(key, result);
                compiled.ingredients().forEach(ingredient -> shapeless.addIngredient(choice(ingredient, loose)));
                shapeless.setGroup(group);
                shapeless.setCategory(craftingCategory(recipe));
                yield shapeless;
            }
            case STONECUTTING -> {
                var stonecutting = new StonecuttingRecipe(key, result, choice(compiled.input(), loose));
                stonecutting.setGroup(group);
                yield stonecutting;
            }
            default -> {
                var input = choice(compiled.input(), loose);
                float xp = recipe.getExperience();
                int time = recipe.getCookingTime();
                CookingRecipe<?> cooking = switch (recipe.getType()) {
                    case FURNACE -> new FurnaceRecipe(key, result, input, xp, time);
                    case BLASTING -> new BlastingRecipe(key, result, input, xp, time);
                    case SMOKING -> new SmokingRecipe(key, result, input, xp, time);
                    case CAMPFIRE -> new CampfireRecipe(key, result, input, xp, time);
                    default -> throw new IllegalStateException(recipe.getType().name());
                };
                cooking.setGroup(group);
                if (recipe.getCategory() != null) {
                    cooking.setCategory(CookingBookCategory.valueOf(recipe.getCategory().toUpperCase(Locale.ROOT)));
                } else if (recipe.getType() == RecipeType.SMOKING) {
                    cooking.setCategory(CookingBookCategory.FOOD);
                }
                yield cooking;
            }
        };
    }

    private static CraftingBookCategory craftingCategory(GardensRecipe recipe) {
        return recipe.getCategory() == null ? CraftingBookCategory.MISC : CraftingBookCategory.valueOf(recipe.getCategory().toUpperCase(Locale.ROOT));
    }

    private RecipeChoice choice(ResolvedIngredient ingredient, boolean loose) {
        if (loose || ingredient.customIds().isEmpty()) {
            var materials = new LinkedHashSet<>(ingredient.materials());
            for (var id : ingredient.customIds()) {
                materials.add(Objects.requireNonNull(GardensFurniture.itemRegistry.getItem(id)).getMaterial());
            }
            return new RecipeChoice.MaterialChoice(new ArrayList<>(materials));
        }
        var stacks = new ArrayList<ItemStack>();
        for (var id : ingredient.customIds()) stacks.add(GardensFurniture.itemRegistry.buildItem(id));
        for (var material : ingredient.materials()) stacks.add(new ItemStack(material));
        return new RecipeChoice.ExactChoice(stacks);
    }

    /** The vanilla recipe the loose copy's grid would collide with, if any. */
    @Nullable
    private NamespacedKey vanillaClash(Compiled compiled, World world) {
        var matrix = new ItemStack[9];
        var recipe = compiled.recipe();
        if (recipe.getType() == RecipeType.SHAPED) {
            for (int y = 0; y < compiled.shape().size(); y++) {
                var row = compiled.shape().get(y);
                for (int x = 0; x < row.length(); x++) {
                    if (row.charAt(x) != ' ') matrix[y * 3 + x] = sample(compiled.key().get(row.charAt(x)));
                }
            }
        } else if (recipe.getType() == RecipeType.SHAPELESS) {
            for (int i = 0; i < compiled.ingredients().size(); i++) matrix[i] = sample(compiled.ingredients().get(i));
        } else {
            return null;
        }
        var found = Bukkit.getCraftingRecipe(matrix, world);
        return found instanceof Keyed keyed ? keyed.getKey() : null;
    }

    private ItemStack sample(ResolvedIngredient ingredient) {
        if (!ingredient.customIds().isEmpty()) {
            var id = ingredient.customIds().iterator().next();
            return new ItemStack(Objects.requireNonNull(GardensFurniture.itemRegistry.getItem(id)).getMaterial());
        }
        return new ItemStack(ingredient.materials().iterator().next());
    }

    // ---- crafting ----

    public static ItemKey keyOf(@Nullable ItemStack stack) {
        if (ItemUtil.isEmpty(stack)) return null;
        var id = NBT.get(stack, nbt -> {
            return nbt.getString(ItemRegistry.FURNITURE_KEY);
        });
        return id == null || id.isEmpty() ? ItemKey.vanilla(stack.getType()) : ItemKey.custom(stack.getType(), id);
    }

    /** The enabled crafting recipe these grid contents make, checking GardensFurniture ids. */
    @Nullable
    public Compiled matchCrafting(ItemStack[] matrix) {
        int width = matrix.length == 4 ? 2 : 3;
        var grid = new ItemKey[matrix.length];
        boolean any = false;
        for (int i = 0; i < matrix.length; i++) {
            grid[i] = keyOf(matrix[i]);
            any |= grid[i] != null;
        }
        if (!any) return null;
        for (var compiled : crafting) {
            if (compiled.matches(grid, width)) return compiled;
        }
        return null;
    }

    public boolean isOurs(NamespacedKey key) {
        return registeredKeys.contains(key);
    }

    public boolean isLoose(NamespacedKey key) {
        return looseKeys.contains(key);
    }

    @Nullable
    public Compiled getCompiled(String id) {
        return compiledById.get(id);
    }

    /** Book recipes in, loose copies out (crafting with one unlocks it like any recipe). */
    public void updateRecipeBook(Player player) {
        if (!looseKeys.isEmpty()) player.undiscoverRecipes(looseKeys);
        if (!bookKeys.isEmpty()) player.discoverRecipes(bookKeys);
    }

    public void hideLooseRecipes(Player player) {
        if (!looseKeys.isEmpty()) player.undiscoverRecipes(looseKeys);
    }

    // ---- fuel ----

    /** Ticks this stack burns for as a custom fuel, 0 when it isn't one. */
    public int fuelTicks(@Nullable ItemStack stack) {
        var key = keyOf(stack);
        if (key == null) return 0;
        if (key.isCustom()) return customFuels.getOrDefault(key.customId(), 0);
        return vanillaFuels.getOrDefault(key.material(), 0);
    }

    public boolean hasCustomFuels() {
        return !customFuels.isEmpty() || !vanillaFuels.isEmpty();
    }

    // ---- editing ----

    /** Writes the recipe into its file (recipes/custom.yml for a new one) and reloads every recipe. */
    public void save(GardensRecipe recipe) throws IOException {
        recipe.validate();
        if (recipe.getFile() == null) recipe.setFile(DEFAULT_FILE);
        var file = new File(recipesDir(), recipe.getFile());
        file.getParentFile().mkdirs();
        var config = YamlConfiguration.loadConfiguration(file);
        config.set(recipe.getId(), null);
        recipe.writeTo(config.createSection(recipe.getId()));
        config.save(file);
        reload();
    }

    public boolean delete(String id) throws IOException {
        var recipe = recipes.get(id);
        if (recipe == null || recipe.getFile() == null) return false;
        var file = new File(recipesDir(), recipe.getFile());
        var config = YamlConfiguration.loadConfiguration(file);
        config.set(id, null);
        config.save(file);
        reload();
        return true;
    }

    /** Writes many recipes at once (the importer), grouped by their file, then reloads. */
    public void saveAll(Collection<GardensRecipe> toSave) throws IOException {
        var byFile = new LinkedHashMap<String, List<GardensRecipe>>();
        for (var recipe : toSave) {
            byFile.computeIfAbsent(Objects.requireNonNullElse(recipe.getFile(), DEFAULT_FILE), f -> new ArrayList<>()).add(recipe);
        }
        for (var entry : byFile.entrySet()) {
            var file = new File(recipesDir(), entry.getKey());
            file.getParentFile().mkdirs();
            var config = YamlConfiguration.loadConfiguration(file);
            for (var recipe : entry.getValue()) {
                config.set(recipe.getId(), null);
                ConfigurationSection section = config.createSection(recipe.getId());
                recipe.writeTo(section);
            }
            config.save(file);
        }
        reload();
    }

    /** Vanilla item tags that contain this material, for the editor's "any of" options. */
    public List<NamespacedKey> vanillaTagsOf(Material material) {
        var tags = new ArrayList<NamespacedKey>();
        for (var tag : Bukkit.getTags(Tag.REGISTRY_ITEMS, Material.class)) {
            if (tag.isTagged(material)) tags.add(tag.getKey());
        }
        tags.sort(Comparator.comparing(NamespacedKey::toString));
        return tags;
    }

    /** recipe_tags.yml tags that contain this item, for the editor's "any of" options. */
    public List<String> customTagsOf(ItemKey item) {
        var tags = new ArrayList<String>();
        for (var name : customTags.keySet()) {
            try {
                if (resolve(Ingredient.of(ItemRef.tag(ItemRef.CUSTOM_NAMESPACE + ":" + name))).test(item)) tags.add(name);
            } catch (Exception ignored) {
                // broken tag, already reported on load
            }
        }
        Collections.sort(tags);
        return tags;
    }
}
