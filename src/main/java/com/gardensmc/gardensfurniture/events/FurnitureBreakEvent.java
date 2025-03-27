package com.gardensmc.gardensfurniture.events;

import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.itementity.ItemEntity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

public class FurnitureBreakEvent extends Event implements Cancellable {

    private Player player;
    private CustomPlaceableItem customItem;
    private ItemEntity itemEntity;
    private @Nullable Interaction hitbox;

    private boolean cancelled;

    private static final HandlerList HANDLERS = new HandlerList();

    public FurnitureBreakEvent(Player player, CustomPlaceableItem customItem, ItemEntity itemEntity) {
        this.player = player;
        this.customItem = customItem;
        this.itemEntity = itemEntity;
    }

    public FurnitureBreakEvent(Player player, CustomPlaceableItem customItem, ItemEntity itemEntity, Interaction interaction) {
        this.player = player;
        this.customItem = customItem;
        this.itemEntity = itemEntity;
        this.hitbox = interaction;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public CustomPlaceableItem getCustomItem() {
        return customItem;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemEntity getItemEntity() {
        return itemEntity;
    }

    public @Nullable Interaction getHitbox() {
        return hitbox;
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
