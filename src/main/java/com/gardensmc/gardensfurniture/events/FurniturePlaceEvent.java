package com.gardensmc.gardensfurniture.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import javax.annotation.Nullable;

public class FurniturePlaceEvent extends Event {

    private final Player player;
    @Getter
    private final Furniture furniture;

    private static final HandlerList HANDLERS = new HandlerList();


    public FurniturePlaceEvent(Player player, Furniture furniture) {
        this.player = player;
        this.furniture = furniture;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Nullable
    public Player getPlayer() {
        return player;
    }

}
