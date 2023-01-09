package com.gardensmc.gardensfurniture.events;

import com.gardensmc.gardensfurniture.custom.CustomItem;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CustomBlockBreakEvent extends Event {

    private Player player;
    private CustomItem customItem;

    private Block block;

    private static final HandlerList HANDLERS = new HandlerList();


    public CustomBlockBreakEvent(Player player, CustomItem customItem, Block block) {
        this.player = player;
        this.customItem = customItem;
        this.block = block;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public CustomItem getCustomItem() {
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
