package com.gardensmc.gardensfurniture.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Fired just before the items a player gets from breaking a custom block or furniture are dropped,
 * like vanilla's BlockDropItemEvent (which custom blocks don't go through, as they drop their items
 * themselves). Change the list to change what drops, or cancel to hand them out another way, as
 * GardensEnchants' Siphon does by putting them straight into the player's inventory.
 */
public class CustomDropItemEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Location location;
    private final List<ItemStack> items;
    private boolean cancelled;

    public CustomDropItemEvent(Player player, Location location, List<ItemStack> items) {
        this.player = player;
        this.location = location;
        this.items = items;
    }

    public Player getPlayer() {
        return player;
    }

    public Location getLocation() {
        return location;
    }

    /**
     * The items about to drop. Mutable.
     */
    public List<ItemStack> getItems() {
        return items;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
