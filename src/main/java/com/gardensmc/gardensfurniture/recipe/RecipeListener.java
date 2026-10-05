package com.gardensmc.gardensfurniture.recipe;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.util.ItemUtil;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class RecipeListener implements Listener {

    /**
     * The server found a recipe for the grid; decide what it really makes by GardensFurniture id. Our
     * recipes win over vanilla ones for the same grid, the way CustomCrafting's did.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        if (e.isRepair() || !(e.getRecipe() instanceof Keyed keyed)) {
            // no server recipe: a result set here would hand the ingredients back when taken
            return;
        }
        var manager = RecipeManager.INSTANCE;
        var match = manager.matchCrafting(e.getInventory().getMatrix());
        if (match != null && match.recipe().isVanillaCopy() && !manager.isOurs(keyed.getKey())) {
            // vanilla's own recipe matched, so only vanilla items are in the grid: vanilla's result stands
            return;
        }
        if (match != null) {
            var result = manager.buildResult(match.recipe());
            if (match.recipe().isCopyContents()) copyContents(e.getInventory().getMatrix(), result);
            e.getInventory().setResult(result);
        } else if (manager.isOurs(keyed.getKey())) {
            // a loose copy matched on base materials only: plain paper isn't a quilt
            e.getInventory().setResult(null);
        }
    }

    // a shulker box's items go into the backpack made from it
    private static void copyContents(ItemStack[] matrix, ItemStack result) {
        for (var item : matrix) {
            if (!ItemUtil.isEmpty(item) && item.hasData(DataComponentTypes.CONTAINER)) {
                result.setData(DataComponentTypes.CONTAINER, item.getData(DataComponentTypes.CONTAINER));
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        if (e.getRecipe() instanceof Keyed keyed && RecipeManager.INSTANCE.isHiddenFromBook(keyed.getKey())
                && e.getWhoClicked() instanceof Player player) {
            // crafting unlocks the recipe that matched; keep hidden ones (loose copies: plain paper icons) out of the book
            Bukkit.getScheduler().runTask(GardensFurniture.plugin, () -> RecipeManager.INSTANCE.hideHiddenRecipes(player));
        }
    }

    // taking smelted items unlocks the furnace recipes used, the same way
    @EventHandler(priority = EventPriority.MONITOR)
    public void onFurnaceExtract(FurnaceExtractEvent e) {
        var player = e.getPlayer();
        Bukkit.getScheduler().runTask(GardensFurniture.plugin, () -> RecipeManager.INSTANCE.hideHiddenRecipes(player));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        RecipeManager.INSTANCE.updateRecipeBook(e.getPlayer());
    }
}
