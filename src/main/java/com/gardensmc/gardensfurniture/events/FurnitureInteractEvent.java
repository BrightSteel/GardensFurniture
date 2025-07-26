package com.gardensmc.gardensfurniture.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class FurnitureInteractEvent extends Event implements Cancellable {
    private Player player;

    private Furniture furniture;

    private EquipmentSlot hand;

    private ItemStack heldItem;

    private boolean cancelled;

    private static final HandlerList HANDLERS = new HandlerList();


    public FurnitureInteractEvent(Player player, Furniture furniture, EquipmentSlot hand, ItemStack heldItem) {
        this.player = player;
        this.furniture = furniture;
        this.hand = hand;
        this.heldItem = heldItem;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Player getPlayer() {
        return player;
    }

    public Furniture getFurniture() {
        return furniture;
    }

    public ItemStack getHeldItem() {
        return heldItem;
    }

    public EquipmentSlot getHand() {
        return hand;
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
