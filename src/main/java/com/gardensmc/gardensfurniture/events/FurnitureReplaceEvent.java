package com.gardensmc.gardensfurniture.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;

/**
 * A block.interactions entry has swapped placed furniture for another piece (a lantern lit, a statue filled), after
 * the swap and after what was held was used up or given back. For a plugin that keeps its own state on the held item
 * (a watering can's water, say) to take its share.
 */
public class FurnitureReplaceEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final EquipmentSlot hand;
    private final String from;
    private final String to;

    public FurnitureReplaceEvent(Player player, EquipmentSlot hand, String from, String to) {
        this.player = player;
        this.hand = hand;
        this.from = from;
        this.to = to;
    }

    public Player getPlayer() {
        return player;
    }

    /** The hand the player clicked with; what it holds now is after the interaction's consume and give. */
    public EquipmentSlot getHand() {
        return hand;
    }

    /** The custom item id of the piece that was there. */
    public String getFrom() {
        return from;
    }

    /** The custom item id of the piece it became. */
    public String getTo() {
        return to;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
