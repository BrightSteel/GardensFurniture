package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.events.ClaimPermissionCheckEvent;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class GriefPreventionListener implements Listener {

    // prevents custom blocks from requiring build permissions to interact with
    @EventHandler
    public void onClaimPermissionCheckEvent(ClaimPermissionCheckEvent e) {
        if (e.getRequiredPermission() == ClaimPermission.Build && e.getTriggeringEvent() instanceof PlayerInteractEvent interactEvent) {
            var block = interactEvent.getClickedBlock();
            if (block != null && block.getType() == Material.NOTE_BLOCK) {
                var customItem = CustomBlockManager.getItemFromLeaves(block);
                if (customItem != null) {
                    e.setDenialReason(() -> null);
                }
            }
        }
    }
}
