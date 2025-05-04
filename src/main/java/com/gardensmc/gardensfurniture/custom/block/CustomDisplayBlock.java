package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.type.Facing;
import com.gardensmc.gardensfurniture.custom.type.Rotation;
import com.gardensmc.gardensfurniture.events.CustomBlockPlaceEvent;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.util.LightUtil;
import com.ticxo.modelengine.api.ModelEngineAPI;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.NBTEntity;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static com.gardensmc.gardensfurniture.register.ItemRegistry.FURNITURE_KEY;

public class CustomDisplayBlock extends CustomBlock {

    protected Hitbox hitbox = Hitbox.INTERACTION;
    private double hitboxWidth = 1.0; // only applicable for interaction hitbox
    private double hitboxHeight = 1.0;
    private boolean facePlayer;
    private double scale = 1.0;
    @Getter
    private boolean gravity = true;
    private int light;
    private Vector3f translation;
    private Quaternionf rotation;
    private boolean centerLeft;
    @Getter
    private String modelengine_id;
    private String itemIdentifier;

    public CustomDisplayBlock(ConfigurationSection blockSection, String identifier) {
        super(blockSection);
        ConfigurationSection displaySection = blockSection.getConfigurationSection("display_data");
        if (displaySection == null) return;

        this.facePlayer = displaySection.getBoolean("face_player");
        this.centerLeft = displaySection.getBoolean("center_left");
        this.light = displaySection.getInt("light");
        if (displaySection.contains("scale")) {
            this.scale = displaySection.getDouble("scale");
        }
        if (displaySection.contains("gravity")) {
            this.gravity = displaySection.getBoolean("gravity");
        }
        var translationSection = displaySection.getConfigurationSection("translation");
        if (translationSection != null) {
            translation = new Vector3f(
                    (float) translationSection.getDouble("x"),
                    (float) translationSection.getDouble("y"),
                    (float) translationSection.getDouble("z")
            );
        }
        var rotationSection = displaySection.getConfigurationSection("rotation");
        if (rotationSection != null && rotationSection.contains("angle")) {
            rotation = new Quaternionf().fromAxisAngleDeg(
                    (float) rotationSection.getDouble("x"),
                    (float) rotationSection.getDouble("y"),
                    (float) rotationSection.getDouble("z"),
                    (float) rotationSection.getDouble("angle")
            );
        }

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
        this.modelengine_id = displaySection.getString("modelengine_id");
        this.itemIdentifier = identifier;
    }

    @Override
    public boolean isSolid() {
        return hitbox == Hitbox.BARRIER;
    }

    @Override
    public boolean placeBlock(ItemStack heldItem, CustomPlaceableItem placeableItem, Player player, Location location, BlockFace blockFace, boolean persistent, FurnitureStore furnitureStore) {
        CustomBlockPlaceEvent event = new CustomBlockPlaceEvent(player, placeableItem, location.getBlock());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

//        Color color = null;
//        if (heldItem != null && heldItem.getItemMeta() instanceof LeatherArmorMeta leather) {
//            color = leather.getColor();
//        }
        return placeDisplay(heldItem, player, location, blockFace, null);
    }

    protected boolean placeDisplay(ItemStack item, Player player, Location location, BlockFace blockFace, Color color) {
        var world = location.getWorld();
        if (world == null) return false;

        var rotation = facePlayer ? Rotation.fromPlayerFacing(player.getYaw()) : Rotation.fromBlockFace(blockFace);
        var locationOffset = location.clone().add(0.5, 0.0, 0.5); // move to center of block
        if (centerLeft) {
            locationOffset = getLocationOffset(locationOffset, Facing.fromYaw(rotation.getYaw()));
        }

        locationOffset.setYaw(rotation.getYaw());
        locationOffset.setPitch(rotation.getPitch());

        ItemDisplay display = (ItemDisplay) world.spawnEntity(locationOffset.clone().add(0, 0.5, 0), EntityType.ITEM_DISPLAY);
//        var meta = item.getItemMeta();
//        if (meta != null) {
//            if (color != null && meta instanceof LeatherArmorMeta leatherMeta) {
//                leatherMeta.setColor(color);
//            }
//            item.setItemMeta(meta);
//        }
        if (modelengine_id != null) {
            var model = ModelEngineAPI.createActiveModel(modelengine_id);
            // todo - apply other transformations to meg display
            if (light > 0) {
                model.setBlockLight(light);
                model.getAnimationHandler().
            }
            ModelEngineAPI
                    .createModeledEntity(display)
                    .addModel(model, true);
            NBT.modifyPersistentData(display, nbt -> { nbt.setItemStack("DISPLAY_ITEM", item); });
        } else {
            display.setItemStack(item);
        }

        // transformations
        var transform = display.getTransformation();
        if (scale != 1.0) {
            transform.getScale().set(scale);
            transform.getTranslation().set(0, scale * 0.25, 0);
        }

        if (translation != null) {
            transform.getTranslation().set(translation);
        }
        if (this.rotation != null) {
            transform.getLeftRotation().set(this.rotation);
        }
        // display needs to face downwards
        if (!facePlayer && blockFace == BlockFace.DOWN) {
            transform.getLeftRotation().set(new Quaternionf().fromAxisAngleDeg(1, 0, 0, 180));
        }
        display.setTransformation(transform);

        if (light > 0) {
            display.setBrightness(new Display.Brightness(light, 0));
        }

        spawnHitbox(locationOffset);
        if (getLight() > 0) {
            LightUtil.placeLight(location, getLight(), hitbox == Hitbox.BARRIER);
        }
        return true;
    }

    private Location getLocationOffset(Location location, Facing facing) {
        double shift = (this.hitboxWidth - 1) / 2;
        return switch (facing) {
            case NORTH -> location.subtract(shift, 0, 0);
            case EAST -> location.subtract(0, 0, shift);
            case SOUTH -> location.add(shift, 0, 0);
            case WEST -> location.add(0, 0, shift);
        };
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
}
