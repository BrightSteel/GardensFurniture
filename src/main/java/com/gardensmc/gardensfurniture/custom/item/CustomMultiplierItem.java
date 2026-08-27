package com.gardensmc.gardensfurniture.custom.item;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.block.CustomBlock;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class CustomMultiplierItem extends CustomPlaceableItem {

    List<CustomBlock> blocks = new ArrayList<>();

    public CustomMultiplierItem(String identifier, ConfigurationSection itemSection) {
        super(identifier, itemSection);
        for (int i = 1; i < 100; i++) {
            ConfigurationSection multiplierSection = Objects
                    .requireNonNull(blockSection)
                    .getConfigurationSection(String.valueOf(i));
            if (multiplierSection == null) {
                break;
            }
            blocks.add(GardensFurniture.itemRegistry.customBlockFactory.createCustomBlock(identifier, multiplierSection, identifier + "_" + i));
        }
    }

    @Override
    public boolean placeBlock(Player player, ItemStack heldItem, Location location, PlaceOptions placeOptions, Block clickedBlock) {
        if (!super.placeBlock(player, heldItem, location, placeOptions, clickedBlock)) {
            return false;
        }
        // only supports leaf / trip wire blocks right now
        var multiplier = getBlockMultiplier(clickedBlock, player);
        if (multiplier == -1) {
            return false;
        }
        var placementLocation = player == null || multiplier == 1 ? location : clickedBlock.getLocation(); // need to replace current block if multiplying
        return blocks.get(multiplier - 1).placeBlock(
                player,
                heldItem,
                this,
                placementLocation,
                placeOptions.setMultiplier(multiplier),
                clickedBlock
        );
    }

    // returns -1 if at max already, otherwise 1 through blocks.size() inclusive
    private int getBlockMultiplier(Block clickedBlock, Player player) {
        var furnitureStore = FurnitureStoreHandler.INSTANCE.getFurnitureStore(clickedBlock);
        // existing multiplier block - increment its multiplier
        if (furnitureStore != null && furnitureStore.getItemIdentifier().equals(this.getIdentifier())) {
            // multiplier - shouldn't be null
            int multiplier = furnitureStore.getMultiplier();
            multiplier++;
            if (multiplier > blocks.size()) {
                // already at max multiplier
                return -1; // should this place on top instead ? like new block
            }
            return multiplier;
        } else if (player == null) {
            // placing via terrain gen - lets do a random multiplier
            return ThreadLocalRandom.current().nextInt(blocks.size()) + 1;
        }

        return 1;
    }

    @Override
    public CustomBlock getCustomBlock() {
        return blocks.get(0);
    }

    public CustomBlock getCustomBlock(int multiplier) {
        if (multiplier == 0 || multiplier > blocks.size()) {
            throw new RuntimeException("Invalid multiplier");
        }
        return blocks.get(multiplier - 1);
    }

    @Override
    public List<CustomBlock> getPlaceableBlocks() {
        return blocks;
    }
}
