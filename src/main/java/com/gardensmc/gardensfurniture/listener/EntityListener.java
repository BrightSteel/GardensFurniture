package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.block.CustomCrystalBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomDisplayBlock;
import com.gardensmc.gardensfurniture.custom.item.CustomBlockItem;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.events.FurnitureBreakEvent;
import com.gardensmc.gardensfurniture.itementity.DisplayItemEntity;
import com.gardensmc.gardensfurniture.itementity.FrameItemEntity;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import com.gardensmc.gardensfurniture.util.ProtectionUtil;
import de.tr7zw.nbtapi.NBTEntity;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EntityListener implements Listener {

    @EventHandler
    public void breakItemFrame(HangingBreakEvent e) {
        if (e.isCancelled()) return;

        // prevent non-entities from breaking
        if (e.getCause() != HangingBreakEvent.RemoveCause.ENTITY) {
            if (e.getEntity() instanceof ItemFrame itemFrame) {
                ItemStack itemStack = itemFrame.getItem();
                if (itemStack.getType() != Material.AIR) {
                    CustomItem customItem = GardensFurniture.itemRegistry.getCustomItem(itemStack);
                    if (customItem != null) {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void entityHitInteractionEntity(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) return;

        if (e.getEntity() instanceof Interaction interaction && e.getDamager() instanceof Player player) {
            var display = CustomBlockManager.getDisplayEntity(interaction.getLocation());
            if (display == null) return;

            var item = display.getItemStack();
            if (item == null) return;

            var customItem = GardensFurniture.itemRegistry.getCustomItem(item);
            // chances are if there's an interaction entity it indeed has a hitbox, but might as well check
            if (customItem instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomDisplayBlock displayBlock) {
                FurnitureBreakEvent ev = new FurnitureBreakEvent(player, placeable, new DisplayItemEntity(display), interaction);
                Bukkit.getPluginManager().callEvent(ev);
                if (ev.isCancelled()) {
                    return;
                }
                if (displayBlock.getHitbox() == CustomDisplayBlock.Hitbox.INTERACTION) {
                    e.getEntity().remove();
                    display.remove();
                    var drop = CustomBlockManager.getDrop(placeable);
                    if (drop != null) {
                        display.getWorld().dropItemNaturally(display.getLocation(), drop);
                    }
                }
            }
        }
    }

    @EventHandler
    public void entityBreakItemFrame(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) return;

        if (e.getEntity() instanceof ItemFrame itemFrame) {
            if ( e.getDamager() instanceof Player player) {

                ItemStack itemStack = itemFrame.getItem();
                if (itemStack.getType() != Material.AIR) {
                    CustomItem customItem = GardensFurniture.itemRegistry.getCustomItem(itemStack);
                    if (customItem instanceof CustomPlaceableItem customBlockItem) {
                        e.setCancelled(true);

                        if (!ProtectionUtil.canBuild(e.getEntity().getLocation(), player)) return;
                        FurnitureBreakEvent ev = new FurnitureBreakEvent(player, customBlockItem, new FrameItemEntity(itemFrame));
                        Bukkit.getPluginManager().callEvent(ev);
                        if (ev.isCancelled()) {
                            return;
                        }

                        if (customBlockItem.getCustomBlock() instanceof CustomCrystalBlock) {
                            return; // only breakable by removing amethyst block
                        }

                        NBTEntity nbtEntity = new NBTEntity(itemFrame);
                        String display = null;
                        if (nbtEntity.getPersistentDataContainer().hasKey("custom_name")) {
                            display = nbtEntity.getPersistentDataContainer().getString("custom_name");

                        }
                        itemFrame.remove();
                        if (!customBlockItem.getCustomBlock().cancelDrop()) {
                            if (display != null && itemStack.getItemMeta() != null) {
                                ItemMeta itemMeta = itemStack.getItemMeta();
                                itemMeta.setDisplayName(display);
                                itemStack.setItemMeta(itemMeta);
                            }
                            itemFrame.getWorld().dropItemNaturally(itemFrame.getLocation(), itemStack);
                        }

                    }
                }
            }
            else {
                e.setCancelled(true);
            }

        }
    }
}
