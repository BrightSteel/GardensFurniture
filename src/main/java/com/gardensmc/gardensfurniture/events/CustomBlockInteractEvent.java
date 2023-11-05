package com.gardensmc.gardensfurniture.events;

import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class CustomBlockInteractEvent extends Event {

    private Player player;
    private CustomItem customItem;

    private EquipmentSlot hand;

    private ItemStack heldItem;

    private Block block;

    private static final HandlerList HANDLERS = new HandlerList();


    public CustomBlockInteractEvent(Player player, CustomItem customItem, EquipmentSlot hand, ItemStack heldItem, Block block) {
        this.player = player;
        this.customItem = customItem;
        this.hand = hand;
        this.heldItem = heldItem;
        this.block = block;
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

    public EquipmentSlot getHand() {
        return hand;
    }

    public ItemStack getHeldItem() {
        return heldItem;
    }

    public Block getBlock() {
        return block;
    }
}
