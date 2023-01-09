package com.gardensmc.gardensfurniture.events;

import com.gardensmc.gardensfurniture.custom.CustomItem;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class FurnitureInteractEvent extends Event {
    private Player player;
    private CustomItem customItem;

    private ItemFrame itemFrame;

    private EquipmentSlot hand;

    private ItemStack heldItem;

    private static final HandlerList HANDLERS = new HandlerList();


    public FurnitureInteractEvent(Player player, CustomItem customItem, ItemFrame itemFrame, EquipmentSlot hand, ItemStack heldItem) {
        this.player = player;
        this.customItem = customItem;
        this.itemFrame = itemFrame;
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

    public CustomItem getCustomItem() {
        return customItem;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemFrame getItemFrame() {
        return itemFrame;
    }

    public ItemStack getHeldItem() {
        return heldItem;
    }

    public EquipmentSlot getHand() {
        return hand;
    }
}
