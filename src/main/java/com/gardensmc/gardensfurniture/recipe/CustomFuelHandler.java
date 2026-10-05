package com.gardensmc.gardensfurniture.recipe;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.Lightable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * `type: fuel` recipes. The furnace only takes items vanilla knows burn, and a GardensFurniture log is
 * paper underneath, so players' clicks into the fuel slot are done here, and furnaces holding one are
 * lit by hand whenever they run out of burn time with something to smelt.
 *
 * <p>Hoppers beside a furnace feed it custom fuel the same way. A furnace left with custom fuel over a
 * restart starts again once someone opens it or a hopper feeds it.
 */
public class CustomFuelHandler implements Listener {

    public static final CustomFuelHandler INSTANCE = new CustomFuelHandler();
    private static final int FUEL_SLOT = 1;

    private final Set<Location> furnaces = new HashSet<>();

    private CustomFuelHandler() {
    }

    // a vanilla fuel given its own burn_time
    @EventHandler(ignoreCancelled = true)
    public void onBurn(FurnaceBurnEvent e) {
        int ticks = RecipeManager.INSTANCE.fuelTicks(e.getFuel());
        if (ticks > 0) e.setBurnTime(ticks);
    }

    /**
     * A hopper beside a furnace fills its fuel slot, but vanilla only lets fuel in, so move custom fuel
     * across here. The event comes before vanilla's check; a hopper above still fills the input slot.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent e) {
        if (!(e.getDestination() instanceof FurnaceInventory furnace) || !isCustomFuel(furnace, e.getItem())) return;
        var from = e.getSource().getLocation();
        if (from == null || !(from.getBlock().getBlockData() instanceof org.bukkit.block.data.type.Hopper hopper)
                || hopper.getFacing() == BlockFace.DOWN) {
            return;
        }
        e.setCancelled(true);
        var item = e.getItem();
        var fuel = furnace.getFuel();
        if (!ItemUtil.isEmpty(fuel) && (!fuel.isSimilar(item) || fuel.getAmount() >= fuel.getMaxStackSize())) return;
        var source = e.getSource();
        for (int i = 0; i < source.getSize(); i++) {
            var stack = source.getItem(i);
            if (ItemUtil.isEmpty(stack) || !stack.isSimilar(item)) continue;
            stack.setAmount(stack.getAmount() - 1);
            source.setItem(i, stack.getAmount() > 0 ? stack : null);
            if (ItemUtil.isEmpty(fuel)) {
                var one = item.clone();
                one.setAmount(1);
                furnace.setFuel(one);
            } else {
                fuel.setAmount(fuel.getAmount() + 1);
                furnace.setFuel(fuel);
            }
            track(furnace);
            return;
        }
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent e) {
        if (e.getInventory() instanceof FurnaceInventory furnace) track(furnace);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory() instanceof FurnaceInventory furnace)) return;
        var manager = RecipeManager.INSTANCE;
        if (!manager.hasCustomFuels()) return;
        var clickedTop = e.getClickedInventory() == furnace;

        if (clickedTop && e.getSlot() == FUEL_SLOT) {
            var player = e.getWhoClicked();
            var cursor = player.getItemOnCursor();
            if (e.getAction() == InventoryAction.HOTBAR_SWAP && e.getHotbarButton() >= 0) {
                var hotbar = player.getInventory().getItem(e.getHotbarButton());
                if (isCustomFuel(furnace, hotbar)) {
                    e.setCancelled(true);
                    player.getInventory().setItem(e.getHotbarButton(), furnace.getFuel());
                    furnace.setFuel(hotbar);
                }
            } else if (isCustomFuel(furnace, cursor) && (e.isLeftClick() || e.isRightClick()) && !e.isShiftClick()) {
                e.setCancelled(true);
                player.setItemOnCursor(place(furnace, cursor, e.isRightClick()));
            }
        } else if (!clickedTop && e.isShiftClick()) {
            var item = e.getCurrentItem();
            // something smeltable still goes to the input slot as usual
            if (isCustomFuel(furnace, item) && !furnace.canSmelt(item)) {
                e.setCancelled(true);
                e.setCurrentItem(place(furnace, item, false));
            }
        }
        Bukkit.getScheduler().runTask(GardensFurniture.plugin, () -> track(furnace));
    }

    private boolean isCustomFuel(FurnaceInventory furnace, ItemStack stack) {
        return !ItemUtil.isEmpty(stack) && !furnace.isFuel(stack) && RecipeManager.INSTANCE.fuelTicks(stack) > 0;
    }

    /** Puts some of `stack` in the fuel slot the way a click would, and returns what's left of it. */
    private ItemStack place(FurnaceInventory furnace, ItemStack stack, boolean one) {
        var fuel = furnace.getFuel();
        int moving = one ? 1 : stack.getAmount();
        if (ItemUtil.isEmpty(fuel)) {
            var placed = stack.clone();
            placed.setAmount(moving);
            furnace.setFuel(placed);
        } else if (fuel.isSimilar(stack)) {
            moving = Math.min(moving, fuel.getMaxStackSize() - fuel.getAmount());
            if (moving <= 0) return stack;
            fuel.setAmount(fuel.getAmount() + moving);
            furnace.setFuel(fuel);
        } else {
            if (one) return stack;
            furnace.setFuel(stack);
            return fuel; // swapped
        }
        var left = stack.clone();
        left.setAmount(stack.getAmount() - moving);
        return left.getAmount() > 0 ? left : null;
    }

    private void track(FurnaceInventory furnace) {
        var holder = furnace.getHolder();
        if (holder != null && RecipeManager.INSTANCE.fuelTicks(furnace.getFuel()) > 0 && !furnace.isFuel(furnace.getFuel())) {
            furnaces.add(holder.getLocation());
        }
    }

    /** Every tick: light furnaces that ran out of burn time but have custom fuel and work to do. */
    public void tick() {
        if (furnaces.isEmpty()) return;
        var it = furnaces.iterator();
        while (it.hasNext()) {
            var location = it.next();
            if (!location.isChunkLoaded() || !(location.getBlock().getState(false) instanceof Furnace furnace)) {
                it.remove();
                continue;
            }
            var inventory = furnace.getInventory();
            var fuel = inventory.getFuel();
            int ticks = RecipeManager.INSTANCE.fuelTicks(fuel);
            if (ticks <= 0 || inventory.isFuel(fuel)) {
                it.remove();
                continue;
            }
            if (furnace.getBurnTime() > 0) continue;
            var smelting = inventory.getSmelting();
            if (ItemUtil.isEmpty(smelting) || !inventory.canSmelt(smelting)) continue;
            var result = inventory.getResult();
            if (!ItemUtil.isEmpty(result) && result.getAmount() >= result.getMaxStackSize()) continue;

            fuel.setAmount(fuel.getAmount() - 1);
            inventory.setFuel(fuel.getAmount() > 0 ? fuel : null);
            furnace.setBurnTime((short) Math.min(ticks, Short.MAX_VALUE));
            var block = location.getBlock();
            if (block.getBlockData() instanceof Lightable lightable && !lightable.isLit()) {
                lightable.setLit(true);
                block.setBlockData(lightable, false);
            }
        }
    }
}
