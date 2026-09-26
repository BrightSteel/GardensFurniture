package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class EntityListener implements Listener {

    @EventHandler
    public void entityHitInteractionEntity(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) return;
        if (e.getEntity() instanceof Interaction interaction && e.getDamager() instanceof Player player && !player.getInventory().getItemInMainHand().getType().toString().toLowerCase().endsWith("sword")) {
            CustomBlockManager.breakFurniture(player, interaction.getLocation(), false);
        }
    }
}
