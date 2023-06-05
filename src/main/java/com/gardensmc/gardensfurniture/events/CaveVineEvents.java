package com.gardensmc.gardensfurniture.events;

import com.gardensmc.gardensfurniture.custom.CustomCaveVineBlock;
import com.gardensmc.gardensfurniture.custom.CustomCaveVineItem;
import com.gardensmc.gardensfurniture.custom.CustomItem;
import com.gardensmc.gardensfurniture.register.CustomRegister;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.CaveVines;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class CaveVineEvents implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void blockPlaceEvent(BlockPlaceEvent e) {
        if (e.isCancelled()) return;

        ItemStack itemStack = e.getItemInHand();
        if (itemStack.getType() == Material.GLOW_BERRIES) {
            CustomItem customItem = CustomRegister.INSTANCE.itemRegistry.getCustomItem(itemStack);
            if (customItem instanceof CustomCaveVineItem customCaveVineItem) {
                Chunk chunk = e.getBlockPlaced().getChunk();
                chunk.getPersistentDataContainer().set(new NamespacedKey("gardens", e.getBlockPlaced().getX() + "." + e.getBlockPlaced().getY() + "." + e.getBlockPlaced().getZ()), PersistentDataType.STRING, customCaveVineItem.getIdentifier());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onVineGrowth(BlockSpreadEvent e) {
        if (e.isCancelled()) return;
        Block nextBlock = e.getNewState().getBlock();
        Block root = e.getSource();
        Chunk chunk = root.getChunk();
        if (root.getType() != Material.CAVE_VINES && root.getType() != Material.CAVE_VINES_PLANT) return;

        String identifier = chunk.getPersistentDataContainer().get(new NamespacedKey("gardens", root.getX() + "." + root.getY() + "." + root.getZ()), PersistentDataType.STRING);
        if (identifier != null) {
            CustomItem customItem = CustomRegister.INSTANCE.itemRegistry.getItem(identifier);
            if (customItem instanceof CustomCaveVineItem customCaveVineItem) {
                nextBlock.getChunk().getPersistentDataContainer().set(new NamespacedKey("gardens", nextBlock.getX() + "." + nextBlock.getY() + "." + nextBlock.getZ()), PersistentDataType.STRING, customCaveVineItem.getIdentifier());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onRightClickLeaves(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (e.getHand() == null) return;

        if (e.getBlockFace() != BlockFace.DOWN) return;

        // only want to enable placement on leaves
        if (e.getClickedBlock() == null || !e.getClickedBlock().getType().toString().toUpperCase().endsWith("LEAVES")) {
            return;
        }

        Block target = e.getClickedBlock().getRelative(e.getBlockFace());
        if (target.getType() != Material.AIR) return;
        BlockState oldState = target.getState();

        ItemStack itemStack = player.getInventory().getItem(e.getHand());
        if (itemStack == null || itemStack.getType() != Material.GLOW_BERRIES) return;

        CustomItem customItem = CustomRegister.INSTANCE.itemRegistry.getCustomItem(itemStack);
        if (customItem instanceof CustomCaveVineItem customCaveVineItem) {

            // fire preliminary block place event to make sure itll go through
            BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, oldState, e.getClickedBlock(), itemStack, player, true, e.getHand());
            Bukkit.getPluginManager().callEvent(blockPlaceEvent);
            if (blockPlaceEvent.isCancelled()) return;

            // update chunk nbt
            Chunk chunk = target.getChunk();
            chunk.getPersistentDataContainer().set(new NamespacedKey("gardens", target.getX() + "." + target.getY() + "." + target.getZ()), PersistentDataType.STRING, customCaveVineItem.getIdentifier());

            // swing hand
            switch (e.getHand()) {
                case HAND -> e.getPlayer().swingMainHand();
                case OFF_HAND -> e.getPlayer().swingOffHand();
            }

            // place item
            CustomCaveVineBlock b = customCaveVineItem.getPlant();
            target.setType(Material.CAVE_VINES);
            BlockData blockData = target.getBlockData();
            if (blockData instanceof CaveVines caveVines) {
                caveVines.setAge(b.getAge());
            }

            // use up item
            if (e.getPlayer().getGameMode() == GameMode.SURVIVAL) {
                ItemStack usedItem = e.getPlayer().getInventory().getItem(e.getHand());
                if (usedItem != null) {
                    usedItem.setAmount(usedItem.getAmount() - 1);
                    e.getPlayer().getInventory().setItem(e.getHand(), usedItem);
                }
            }
        }
    }
}
