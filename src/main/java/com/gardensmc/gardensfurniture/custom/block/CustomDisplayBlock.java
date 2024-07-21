package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import de.tr7zw.nbtapi.NBTEntity;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.AxisAngle4f;

public class CustomDisplayBlock extends CustomBlock {

    private Hitbox hitbox = Hitbox.NONE;
    private double hitboxWidth = 1.0; // only applicable for interaction hitbox
    private double hitboxHeight = 1.0;

    public CustomDisplayBlock(ConfigurationSection blockSection) {
        super(blockSection);
        ConfigurationSection displaySection = blockSection.getConfigurationSection("display_data");
        if (displaySection == null) return;

        var hitboxSection = displaySection.getConfigurationSection("hitbox");
        if (hitboxSection != null) {
            this.hitbox = Hitbox.valueOf(hitboxSection.getString("type").toUpperCase());
            if (hitboxSection.contains("width")) {
                this.hitboxWidth = hitboxSection.getDouble("width");
            }
            if (hitboxSection.contains("height")) {
                this.hitboxHeight = hitboxSection.getDouble("height");
            }
        }
    }

    @Override
    public boolean placeBlock(ItemStack heldItem, CustomPlaceableItem placeableItem, Player player, Location location, BlockFace blockFace, boolean persistent, FurnitureStore furnitureStore) {
        var world = location.getWorld();
        if (world == null) return false;

        var locationOffset = location.add(0.5, 0.5, 0.5); // move to center of block
        location.setYaw(0);
        location.setPitch(0);
        ItemDisplay display = (ItemDisplay) world.spawnEntity(locationOffset, EntityType.ITEM_DISPLAY);
        ItemStack item = GardensFurniture.itemRegistry.buildItem(placeableItem);
        display.setItemStack(item);
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        var meta = item.getItemMeta();
        if (meta != null) {
            var customName = meta.getDisplayName();
            if (customName != null) {
                NBTEntity nbtEntity = new NBTEntity(display);
                nbtEntity.getPersistentDataContainer().setString("custom_name", customName);
            }
        }

        var rotation = getRotation(blockFace);
        display.setRotation(rotation.getLeft(), rotation.getRight());

        spawnHitbox(locationOffset);
        return true;
    }

    private void spawnHitbox(Location location) {
        if (hitbox == Hitbox.BARRIER) {
            location.getBlock().setType(Material.BARRIER);
        } else if (hitbox == Hitbox.INTERACTION) {
            Interaction interaction = (Interaction) location.getWorld().spawnEntity(location, EntityType.INTERACTION);
            interaction.setInteractionWidth((float) hitboxWidth);
            interaction.setInteractionHeight((float) hitboxHeight);
        }
    }

    public Hitbox getHitbox() {
        return hitbox;
    }

    public enum Hitbox {
        BARRIER,
        INTERACTION,
        NONE
    }

    private Pair<Float, Float> getRotation(BlockFace facing) {
        float yaw = 0f;
        float pitch = 0f;
        switch (facing) {
            case DOWN -> pitch = 90f;
            case UP -> pitch = -90f;
            case SOUTH -> yaw = 0f;
            case NORTH -> yaw = -180;
            case WEST -> yaw = 90;
            case EAST -> yaw = 270;
        }
        return Pair.of(yaw, pitch);
    }
}
