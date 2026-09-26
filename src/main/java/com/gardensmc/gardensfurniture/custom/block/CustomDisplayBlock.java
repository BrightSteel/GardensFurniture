package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.item.PlaceOptions;
import com.gardensmc.gardensfurniture.custom.type.Facing;
import com.gardensmc.gardensfurniture.custom.type.Rotation;
import com.gardensmc.gardensfurniture.events.Furniture;
import com.gardensmc.gardensfurniture.events.FurniturePlaceEvent;
import com.gardensmc.gardensfurniture.util.ProtectionUtil;
import com.ticxo.modelengine.api.ModelEngineAPI;
import de.tr7zw.nbtapi.NBT;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

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
    @Getter
    private String model_override;
    @Getter
    private boolean connectable;
    private List<Vector> barriers = Collections.emptyList();
    // right-click toggles light on/off; placed off, lit_model is shown while on
    @Getter
    private final boolean lightToggleable;
    @Getter
    private String litModel;

    public static final String LIGHT_ON_KEY = "FurnitureLightOn";

    public CustomDisplayBlock(ConfigurationSection blockSection, String identifier, String blockId) {
        super(blockSection, blockId);
        this.lightToggleable = blockSection.getBoolean("toggleable") && getLight() > 0;
        if (blockSection.getBoolean("toggleable") && getLight() <= 0) {
            // block.light places the light blocks (and is what can be toggled); display_data.light only
            // brightens the model, so on its own it leaves nothing to switch
            GardensFurniture.plugin.getLogger().warning(("'%s' is toggleable but has no block light - set block.light "
                    + "(display_data.light only sets model brightness)").formatted(identifier));
        }
        this.itemIdentifier = identifier;
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
            this.barriers = hitboxSection.getStringList("barriers").stream().map(Barriers::parse).toList();
        }
        this.modelengine_id = displaySection.getString("modelengine_id");
        this.model_override = displaySection.getString("model_override");
        this.litModel = displaySection.getString("lit_model");
        this.itemIdentifier = identifier;
        this.connectable = displaySection.getBoolean("connectable");
    }

    @Override
    public boolean isSolid() {
        return hitbox == Hitbox.BARRIER;
    }

    @Override
    public boolean placeBlock(Player player, ItemStack heldItem, CustomPlaceableItem customItem, Location location, PlaceOptions placeOptions, Block clickedBlock) {

        var yaw = player != null ? player.getYaw() : placeOptions.getYaw();
        var placed = placeDisplay(customItem, getItemToDisplay(heldItem), player, yaw, location, placeOptions.getBlockFace());
        if (placed) {
            var block = location.getBlock();
            playPlaceSound(block, null);
        }
        return placed;
    }

    private ItemStack getItemToDisplay(ItemStack heldItem) {
        ItemStack itemStack;
        var heldCustomItem = GardensFurniture.itemRegistry.getCustomItem(heldItem);
        if (heldCustomItem != null && heldCustomItem.getIdentifier().equals(itemIdentifier)) {
            // use held item if possible to conserve display name, lore, etc
            itemStack = heldItem.clone();
            itemStack.setAmount(1);
        } else {
            itemStack = GardensFurniture.itemRegistry.buildItem(itemIdentifier);
        }
        if (itemStack != null && model_override != null) {
            var override = GardensFurniture.itemRegistry.getItem(model_override);
            itemStack.editMeta(meta -> meta.setItemModel(NamespacedKey.fromString(override.getComponents().getItem_model())));
        } else if (itemStack != null && lightToggleable) {
            // always place in the off state, even if the source item carries the lit model
            setDisplayModel(itemStack, false);
        }
        return itemStack;
    }

    private void setDisplayModel(ItemStack itemStack, boolean lit) {
        var customItem = GardensFurniture.itemRegistry.getItem(itemIdentifier);
        var model = lit && litModel != null ? litModel : customItem.getComponents().getItem_model();
        itemStack.editMeta(meta -> meta.setItemModel(NamespacedKey.fromString(model)));
    }

    public boolean isLightOn(ItemDisplay display) {
        if (!lightToggleable) return true;
        return NBT.getPersistentData(display, nbt -> nbt.getBoolean(LIGHT_ON_KEY));
    }

    public void toggleLight(ItemDisplay display) {
        if (!lightToggleable || modelengine_id != null) return;
        boolean lit = !isLightOn(display);
        NBT.modifyPersistentData(display, nbt -> { nbt.setBoolean(LIGHT_ON_KEY, lit); });

        var item = display.getItemStack();
        setDisplayModel(item, lit);
        display.setItemStack(item);
        display.setBrightness(lit && light > 0 ? new Display.Brightness(light, 0) : null);

        if (lit) {
            placeLight(display.getLocation());
        } else {
            removeLight(display.getLocation());
        }
    }

    protected boolean placeDisplay(CustomPlaceableItem customItem, ItemStack item, @Nullable Player player, float yaw, Location location, BlockFace blockFace) {
        var world = location.getWorld();
        if (world == null) return false;

        List<Vector> barrierVectors;
        if (hitbox == Hitbox.BARRIER && !barriers.isEmpty()) {
            var remappedVectors = barriers.stream().map(v -> Barriers.rotate(v, yaw)).toList();
            // every barrier cell must be free, and (for players) buildable and not occupied by a mob or player
            if (remappedVectors.stream().map(vector -> location.clone().add(vector)).allMatch(cell ->
                    CustomBlockManager.isPlaceableMaterial(cell.getBlock().getType())
                            && (player == null || (ProtectionUtil.canBuild(cell, player) && !CustomBlockManager.entityBlocking(cell))))) {
                barrierVectors = remappedVectors;
            } else {
                return false;
            }
        } else {
            barrierVectors = Collections.emptyList();
        }

        var rotation = facePlayer ? Rotation.fromPlayerFacing(yaw) : Rotation.fromBlockFace(blockFace);
        var locationOffset = location.clone().add(0.5, 0.0, 0.5); // move to center of block
        if (centerLeft) {
            locationOffset = getLocationOffset(locationOffset, Facing.fromYaw(rotation.getYaw()));
        }

        locationOffset.setYaw(rotation.getYaw());
        locationOffset.setPitch(rotation.getPitch());

        ItemDisplay display = (ItemDisplay) world.spawnEntity(locationOffset.clone().add(0, 0.5, 0), EntityType.ITEM_DISPLAY);
        if (modelengine_id != null) {
            var model = ModelEngineAPI.createActiveModel(modelengine_id);
            // todo - apply other transformations to meg display
            if (light > 0) {
                model.setBlockLight(light);
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

        if (light > 0 && !lightToggleable) {
            display.setBrightness(new Display.Brightness(light, 0));
        }
        if (lightToggleable) {
            NBT.modifyPersistentData(display, nbt -> { nbt.setBoolean(LIGHT_ON_KEY, false); });
        }

        // hitbox data
        Barriers.write(display, barrierVectors);

        spawnHitbox(locationOffset, barrierVectors);
        if (!lightToggleable) {
            // anchor on the display, like toggling and breaking do (center_left can move it off `location`)
            placeLight(display.getLocation());
        }
        Bukkit.getPluginManager().callEvent(new FurniturePlaceEvent(player, new Furniture(customItem, display, null)));
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

    private void spawnHitbox(Location location, List<Vector> barrierVectors) {
        if (hitbox == Hitbox.BARRIER) {
            if (barrierVectors.isEmpty()) {
                location.getBlock().setType(Material.BARRIER);
            } else {
                barrierVectors.forEach(barrierVector -> location.clone().add(barrierVector).getBlock().setType(Material.BARRIER));
            }
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
