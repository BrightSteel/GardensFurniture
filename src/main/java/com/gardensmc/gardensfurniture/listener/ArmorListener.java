package com.gardensmc.gardensfurniture.listener;

import com.codingforcookies.armorequip.ArmorEquipEvent;
import com.gardensmc.gardensfurniture.GardensFurniture;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;

import java.util.logging.Level;

public class ArmorListener implements Listener {

    private static final String VANILLA_TRIM_OVERRIDE = "chainmail";

    @EventHandler
    public void onPlayerEquip(ArmorEquipEvent e) {
        setVanillaTrimPattern(e.getNewArmorPiece());
    }

    @EventHandler
    public void onArmorStandEquip(PlayerArmorStandManipulateEvent e) {
        setVanillaTrimPattern(e.getArmorStandItem());
    }

    @EventHandler
    public void onTrimArmor(PrepareSmithingEvent e) {
        if (e.getResult() == null) return;
        var armor = getVanillaArmorEquipment(e.getInventory());
        if (armor != null) {
            e.setResult(null); // disable changing trims
        }
    }

    private void setVanillaTrimPattern(ItemStack itemStack) {
        if (itemStack == null || !(itemStack.getItemMeta() instanceof ArmorMeta armorMeta)) return;
        if (!itemStack.getType().toString().toLowerCase().startsWith(VANILLA_TRIM_OVERRIDE)) return;
        if (armorMeta.hasTrim()) return;
        var key = NamespacedKey.fromString(VANILLA_TRIM_OVERRIDE);
        if (key == null) {
            GardensFurniture.plugin.getLogger().log(Level.SEVERE, "Cannot create vanilla armor key");
            return;
        }
        var trimPattern = Registry.TRIM_PATTERN.get(key);
        if (trimPattern == null) {
            GardensFurniture.plugin.getLogger().log(Level.SEVERE, "Cannot find vanilla armor key");
            return;
        }
        armorMeta.setTrim(new ArmorTrim(TrimMaterial.REDSTONE, trimPattern));
        itemStack.setItemMeta(armorMeta);
    }

    private ItemStack getVanillaArmorEquipment(SmithingInventory inventory) {
        for (var itemStack : inventory.getContents()) {
            if (itemStack.getType().toString().toLowerCase().startsWith(VANILLA_TRIM_OVERRIDE)) {
                return itemStack;
            }
        }
        return null;
    }
}
