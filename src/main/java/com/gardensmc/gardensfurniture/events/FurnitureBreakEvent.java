package com.gardensmc.gardensfurniture.events;

import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class FurnitureBreakEvent extends Event implements Cancellable {

    private Player player;
    private CustomItem customItem;

    private ItemFrame itemFrame;

    private boolean cancelled;

    private static final HandlerList HANDLERS = new HandlerList();


    public FurnitureBreakEvent(Player player, CustomItem customItem, ItemFrame itemFrame) {
        this.player = player;
        this.customItem = customItem;
        this.itemFrame = itemFrame;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public CustomItem getCustomItem() {
        return customItem;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemFrame getItemFrame() {
        return itemFrame;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }
}
