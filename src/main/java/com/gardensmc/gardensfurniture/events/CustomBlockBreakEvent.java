package com.gardensmc.gardensfurniture.events;

import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CustomBlockBreakEvent extends Event {

    private Player player;
    private CustomPlaceableItem customItem;

    private Block block;

    private static final HandlerList HANDLERS = new HandlerList();

    public CustomBlockBreakEvent(Player player, CustomPlaceableItem customItem, Block block) {
        this.player = player;
        this.customItem = customItem;
        this.block = block;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public CustomPlaceableItem getCustomItem() {
        return customItem;
    }

    public Player getPlayer() {
        return player;
    }

    public Block getBlock() {
        return block;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
