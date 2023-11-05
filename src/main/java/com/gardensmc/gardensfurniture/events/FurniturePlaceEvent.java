package com.gardensmc.gardensfurniture.events;

import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import javax.annotation.Nullable;

public class FurniturePlaceEvent extends Event {

    private Player player;
    private CustomItem customItem;

    private ItemFrame itemFrame;

    private static final HandlerList HANDLERS = new HandlerList();


    public FurniturePlaceEvent(Player player, CustomItem customItem, ItemFrame itemFrame) {
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

    @Nullable
    public Player getPlayer() {
        return player;
    }

    public ItemFrame getItemFrame() {
        return itemFrame;
    }
}
