package com.gardensmc.gardensfurniture.command;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.menu.RecipeEditorMenu;
import com.gardensmc.gardensfurniture.menu.RecipeListMenu;
import com.gardensmc.gardensfurniture.permission.Permissions;
import com.gardensmc.gardensfurniture.recipe.CustomCraftingImporter;
import com.gardensmc.gardensfurniture.recipe.GardensRecipe;
import com.gardensmc.gardensfurniture.recipe.RecipeManager;
import com.gardensmc.gardensfurniture.recipe.RecipeType;
import com.gardensmc.gardensfurniture.util.CommandSenderUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /furniture recipe [list | create [id] | edit &lt;id&gt; | reload | import [confirm]]
 */
final class RecipeCommand {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private RecipeCommand() {
    }

    static void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.RECIPES)) {
            CommandSenderUtil.sendPermissionError(sender);
            return;
        }
        var sub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";
        switch (sub) {
            case "reload" -> {
                RecipeManager.INSTANCE.reload();
                CommandSenderUtil.sendSuccess(sender, "Reloaded %d recipe(s); errors, if any, are in the console".formatted(RecipeManager.INSTANCE.getRecipes().size()));
            }
            case "import" -> importCustomCrafting(sender, args.length > 2 && args[2].equalsIgnoreCase("confirm"));
            case "list", "create", "edit" -> {
                if (!(sender instanceof Player player)) {
                    CommandSenderUtil.sendPlayerOnly(sender);
                } else if (sub.equals("list")) {
                    new RecipeListMenu(player).open();
                } else if (sub.equals("create") && args.length < 3) {
                    RecipeListMenu.promptNew(player);
                } else if (args.length < 3) {
                    CommandSenderUtil.sendError(sender, "/furniture recipe edit <id>");
                } else {
                    openEditor(player, args[2].toLowerCase(Locale.ROOT), sub.equals("create"));
                }
            }
            default -> CommandSenderUtil.sendError(sender, "/furniture recipe [list|create [id]|edit <id>|reload|import [confirm]]");
        }
    }

    private static void openEditor(Player player, String id, boolean create) {
        var existing = RecipeManager.INSTANCE.getRecipes().get(id);
        if (existing != null) {
            new RecipeEditorMenu(player, existing).open();
        } else if (!create) {
            CommandSenderUtil.sendError(player, "No recipe " + id);
        } else if (!GardensRecipe.isValidId(id)) {
            CommandSenderUtil.sendError(player, "Recipe ids may only use a-z, 0-9, _ . - /");
        } else {
            new RecipeEditorMenu(player, new GardensRecipe(id, RecipeType.SHAPED)).open();
        }
    }

    private static void importCustomCrafting(CommandSender sender, boolean confirm) {
        var folder = new File(GardensFurniture.plugin.getDataFolder().getParentFile(), "CustomCrafting");
        CustomCraftingImporter.Result result;
        try {
            result = new CustomCraftingImporter(folder, new RegistryItems()).read();
        } catch (Exception e) {
            CommandSenderUtil.sendError(sender, "Couldn't read " + folder + ": " + e.getMessage());
            return;
        }
        var existing = RecipeManager.INSTANCE.getRecipes();
        var fresh = result.recipes().stream().filter(r -> !existing.containsKey(r.getId())).toList();
        int alreadyThere = result.recipes().size() - fresh.size();

        result.skipped().forEach(s -> sender.sendMessage(MM.deserialize("<red>Can't import " + escape(s))));
        result.notes().forEach(n -> sender.sendMessage(MM.deserialize("<yellow>" + escape(n))));
        sender.sendMessage(MM.deserialize("<gold>CustomCrafting: <white>%d</white> recipe(s) to import, <white>%d</white> already imported, <white>%d</white> can't be imported."
                .formatted(fresh.size(), alreadyThere, result.skipped().size())));
        if (fresh.isEmpty()) return;
        if (!confirm) {
            sender.sendMessage(MM.deserialize("<gray>Nothing written yet. Run <white>/furniture recipe import confirm</white> to write them to recipes/imported/, "
                    + "then disable CustomCrafting so the two don't both craft them."));
            return;
        }
        try {
            RecipeManager.INSTANCE.saveAll(fresh);
        } catch (Exception e) {
            CommandSenderUtil.sendError(sender, "Couldn't write the recipes: " + e.getMessage());
            return;
        }
        var loaded = fresh.stream().filter(r -> !r.isEnabled() || r.getType() == RecipeType.FUEL
                || RecipeManager.INSTANCE.getCompiled(r.getId()) != null).count();
        CommandSenderUtil.sendSuccess(sender, "Imported %d recipe(s) into recipes/imported/; %d loaded%s".formatted(fresh.size(), loaded,
                loaded < fresh.size() ? " (see the console for the rest)" : ""));
    }

    private static String escape(String s) {
        return MM.escapeTags(s);
    }

    private static class RegistryItems implements CustomCraftingImporter.Items {
        @Override
        public boolean exists(String id) {
            return GardensFurniture.itemRegistry.getItem(id) != null;
        }

        @Nullable
        @Override
        public String idForDisplayName(String name) {
            var matches = new ArrayList<String>();
            GardensFurniture.itemRegistry.getCustomItems().forEach((id, item) -> {
                if (item.getDisplay() != null && MM.stripTags(item.getDisplay()).trim().equalsIgnoreCase(name)) matches.add(id);
            });
            return matches.size() == 1 ? matches.getFirst() : null;
        }
    }

    static List<String> tabComplete(String[] args) {
        if (args.length == 2) return List.of("list", "create", "edit", "reload", "import");
        if (args.length == 3 && args[1].equalsIgnoreCase("edit")) {
            return RecipeManager.INSTANCE.getRecipes().keySet().stream().filter(id -> id.startsWith(args[2].toLowerCase(Locale.ROOT))).sorted().toList();
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("import")) return List.of("confirm");
        return List.of();
    }
}
