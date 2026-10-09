package com.gardensmc.gardensfurniture.custom.block;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.custom.item.PlaceOptions;
import com.gardensmc.gardensfurniture.events.CustomBlockPlaceEvent;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import com.gardensmc.gardensfurniture.util.SpecialItems;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CustomTripwireBlock extends CustomBlock {

    private final boolean powered;
    private final boolean attached;
    private final boolean north;
    private final boolean south;
    private final boolean east;
    private final boolean west;
    private final boolean disarmed;
    private final boolean large;

    public CustomTripwireBlock(ConfigurationSection blockSection, String customBlockName, String blockId) {
        super(blockSection, blockId);
        // walked through on Java, so on Bedrock too
        if (getBedrock() != null) getBedrock().setPassable(true);
        ConfigurationSection tripWireSection = getConfigSectionOrThrow(blockSection, "tripwire_data", customBlockName);
        this.north = tripWireSection.getBoolean("north");
        this.south = tripWireSection.getBoolean("south");
        this.east = tripWireSection.getBoolean("east");
        this.west = tripWireSection.getBoolean("west");
        this.disarmed = tripWireSection.getBoolean("disarmed");
        this.powered = tripWireSection.getBoolean("powered");
        this.attached = tripWireSection.getBoolean("attached");
        this.large = blockSection.getBoolean("large");
    }

    public boolean isNorth() {
        return north;
    }

    public boolean isSouth() {
        return south;
    }

    public boolean isEast() {
        return east;
    }

    public boolean isWest() {
        return west;
    }

    public boolean isDisarmed() {
        return disarmed;
    }

    public boolean isPowered() {
        return powered;
    }

    public boolean isAttached() {
        return attached;
    }

    public boolean isLarge() {
        return large;
    }

    @Override
    public boolean isSolid() {
        return true;
    }

    @Override
    public boolean placeBlock(Player player, ItemStack heldItem, CustomPlaceableItem customItem, Location location, PlaceOptions placeOptions, Block clickedBlock) {
        Block block = location.getBlock();
        var blockFace = placeOptions.getBlockFace();

        if (isLarge()) {
            // fail if not placing on ground since it is easiest to ensure top block is removed this way
            if (blockFace != BlockFace.UP) {
                return false;
            }

            // ensure air block above
            Block top = block.getRelative(blockFace);
            if (top.getType() != Material.AIR) return false;

            // ensure tripwire top is registered
            if (GardensFurniture.itemRegistry.getItem(SpecialItems.TRIPWIRE_TOP) == null) {
                return false;
            }

            // place blank tripwire as hitbox
            top.setType(Material.TRIPWIRE);
            FurnitureStoreHandler.INSTANCE.addFurnitureStore(top, new FurnitureStore(SpecialItems.TRIPWIRE_TOP), heldItem != null);
        }


        block.setType(Material.TRIPWIRE);
        FurnitureStoreHandler.INSTANCE.addFurnitureStore(
                block,
                FurnitureStore.builder()
                        .itemIdentifier(customItem.getIdentifier())
                        .multiplier(placeOptions.getMultiplier())
                        .facing(placeOptions.getFacing())
                        .build(),
                heldItem != null
        );

        Bukkit.getPluginManager().callEvent(new CustomBlockPlaceEvent(player, customItem, block));
        // play place sound
        playPlaceSound(block, null);
        return true;
    }
}
