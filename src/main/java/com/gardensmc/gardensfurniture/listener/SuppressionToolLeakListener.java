package com.gardensmc.gardensfurniture.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;

/**
 * Keeps the client-only mining rule (BlockBreakingListener#withBreakSuppressionTool) off real items. In creative the
 * client sends its copies of items to the server, and those carry the rule: it's taken off as they arrive, and off
 * anything a player already had it saved on when they join.
 */
public class SuppressionToolLeakListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCreativeItem(InventoryCreativeEvent e) {
        var item = e.getCursor();
        var clean = BlockBreakingListener.withoutBreakSuppressionTool(item);
        if (clean != item) e.setCursor(clean);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        clean(player.getInventory());
        clean(player.getEnderChest());
    }

    private static void clean(Inventory inventory) {
        var contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            var item = contents[slot];
            var clean = BlockBreakingListener.withoutBreakSuppressionTool(item);
            if (clean != item) inventory.setItem(slot, clean);
        }
    }
}
