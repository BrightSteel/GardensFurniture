package com.gardensmc.gardensfurniture.custom.item.mechanics;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.WrappedItem;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

@AllArgsConstructor
public class CaptureMobHandler {

    public static final Set<EntityType> CAPTURABLE_MOB_FILTER = Set.of(
            EntityType.PLAYER,
            EntityType.ARMOR_STAND,
            EntityType.ENDER_DRAGON
    );
    private static final String CAPTURED_GROUP_KEY = "CaptureMob";
    private static final String ENTITY_TYPE_KEY = "EntityType";
    private static final String ENTITY_NBT_KEY = "EntityNBT";
    private static final Set<String> ENTITY_NBT_FILTER = Set.of(
            "id",
            "UUID",
            "Pos",
            "Rotation",
            "Motion",
            "OnGround",
            "leash"
    );

    private final Player player;
    private final WrappedItem heldItem;

    public boolean isCaptureItem() {
        var mechanics = heldItem.customItem().getMechanics();
        return mechanics != null && mechanics.isCaptureMobs();
    }

    public boolean canCaptureMob(Entity entity) {
        var captureMobsMechanic = heldItem.customItem().getMechanics().getCapture_mobs();
        return !captureMobsMechanic.getDisallowed_types().contains(entity.getType()) && !isStoringMob();
    }

    public void captureMob(LivingEntity mob) {
        var entityNbt = getEntityNBT(mob);
        var mobType = mob.getType().toString();
        // replace with mob egg
        var heldItemNBT = NBT.createNBTObject();
        NBT.get(heldItem.itemStack(), heldItemNBT::mergeCompound);
        var egg = new ItemStack(Material.valueOf(mobType + "_SPAWN_EGG"));
        NBT.modify(egg, nbt -> {
            nbt.mergeCompound(heldItemNBT);
        });
        egg.editMeta(meta -> {
            meta.displayName(heldItem.itemStack().getItemMeta().displayName());
            var lore = new ArrayList<Component>();
            lore.add(MiniMessage.miniMessage().deserialize("<!italic><blue>Captured Mob: <gray>" + mobType));
            lore.add(MiniMessage.miniMessage().deserialize("<!italic><gray>Right click to release"));
            meta.lore(lore);
        });
        NBT.modify(egg, nbt -> {
            var capturedGroupNBT = nbt.getOrCreateCompound(CAPTURED_GROUP_KEY);
            capturedGroupNBT.setString(ENTITY_TYPE_KEY, mobType);
            capturedGroupNBT.getOrCreateCompound(ENTITY_NBT_KEY).mergeCompound(entityNbt);
        });
        player.getInventory().setItemInMainHand(egg);
        mob.remove();
    }

    public boolean isStoringMob() {
        return NBT.get(heldItem.itemStack(), nbt -> (boolean) nbt.hasTag(CAPTURED_GROUP_KEY));
    }

    public void releaseMob(Location location) {
        var storedCapture = getStoredCapture();
        var entityType = EntityType.valueOf(storedCapture.entityType);

        var entity = location.getWorld().spawnEntity(location, entityType);
        // restore NBT
        NBT.modify(entity, nbt -> {
            nbt.mergeCompound(storedCapture.entityNBT);
        });
        removeStoredCaptureNBT();
        // replace with default egg
        var egg = GardensFurniture.itemRegistry.buildItem(heldItem.customItem());
        egg.editMeta(meta -> {
            meta.displayName(heldItem.itemStack().getItemMeta().displayName());
        });
       player.getInventory().setItemInMainHand(egg);
    }

    private ReadWriteNBT getEntityNBT(Entity entity) {
        var entityNbt = NBT.createNBTObject();
        NBT.get(entity, entityNbt::mergeCompound);
        // filter out unneeded data
        ENTITY_NBT_FILTER.forEach(entityNbt::removeKey);
        return entityNbt;
    }

    private StoredCapture getStoredCapture() {
        return NBT.get(heldItem.itemStack(), nbt -> {
            var captureGroupNBT = Objects.requireNonNull(nbt.getCompound(CAPTURED_GROUP_KEY));
            var entityNBT = NBT.createNBTObject();
            entityNBT.mergeCompound(captureGroupNBT.getCompound(ENTITY_NBT_KEY));
            return new StoredCapture(captureGroupNBT.getString(ENTITY_TYPE_KEY), entityNBT);
        });
    }

    private void removeStoredCaptureNBT() {
        NBT.modify(heldItem.itemStack(), nbt -> {
            nbt.removeKey(CAPTURED_GROUP_KEY);
        });
    }

    private record StoredCapture(String entityType, ReadableNBT entityNBT) {
    }
}
