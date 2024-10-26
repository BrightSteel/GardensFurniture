package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.item.CustomBlockItem;
import com.gardensmc.gardensfurniture.custom.item.CustomItem;
import com.gardensmc.gardensfurniture.custom.item.CustomPlaceableItem;
import com.gardensmc.gardensfurniture.events.CustomBlockInteractEvent;
import com.gardensmc.gardensfurniture.events.FurnitureInteractEvent;
import com.gardensmc.gardensfurniture.util.ProtectionUtil;
import dev.geco.gsit.api.GSitAPI;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.EnumHand;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R1.inventory.CraftItemStack;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.concurrent.CompletableFuture;

public class PlayerListener implements Listener {

    @EventHandler
    public void onItemInteract(PlayerInteractEvent e) {


        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.isCancelled()) return;
        if (e.getClickedBlock() == null) return;

        // cancel all note block right clicks that are custom blocks
        if (e.getClickedBlock().getType() == Material.NOTE_BLOCK && e.getClickedBlock().getBlockData() instanceof NoteBlock noteBlock) {
            CustomItem customItem = CustomBlockManager.getItemFromNoteBlock(noteBlock);
            if (customItem != null) {
                e.setCancelled(true);
                if (!ProtectionUtil.canBuild(e.getClickedBlock().getLocation(), e.getPlayer())) return;
                Bukkit.getPluginManager().callEvent(new CustomBlockInteractEvent(e.getPlayer(), customItem, e.getHand(), e.getItem(), e.getClickedBlock()));
                // if player is holding an item with an action, lets place it
                if (e.getHand() != null) {
                    ItemStack stack = e.getPlayer().getInventory().getItem(e.getHand());
                    if (stack == null || stack.getType() == Material.AIR) return;
                    net.minecraft.world.item.ItemStack craftItemStack = CraftItemStack.asNMSCopy(stack);
                    EntityPlayer human = ((CraftPlayer) e.getPlayer()).getHandle();
                    EnumHand hand = EnumHand.a; // main hand
                    EnumDirection direction;
                    switch (e.getBlockFace()) {
                        case SOUTH -> direction = EnumDirection.d;
                        case WEST -> direction = EnumDirection.e;
                        case NORTH -> direction = EnumDirection.c;
                        case EAST -> direction = EnumDirection.f;
                        case DOWN -> direction = EnumDirection.a;
                        default -> direction = EnumDirection.b;
                    }

                    craftItemStack.a(new ItemActionContext(human, hand, new MovingObjectPositionBlock(
                            new Vec3D(e.getClickedBlock().getX(), e.getClickedBlock().getY(), e.getClickedBlock().getZ()),
                            direction,
                            new BlockPosition(e.getClickedBlock().getX(), e.getClickedBlock().getY(), e.getClickedBlock().getZ()),
                            false
                    )));
//


                }
            }
        }

        ItemStack itemStack = e.getItem();
        if (itemStack == null) return;
        if (e.getHand() == null) return;

        // handle placement of custom items
        Location clickedLocation = e.getClickedBlock().getLocation();
        Location location = getLocationFromBlockFace(clickedLocation, e.getBlockFace());
        if (!CustomBlockManager.isPlaceableMaterial(location.getBlock().getType())) return;

        CustomItem customItem = GardensFurniture.itemRegistry.getCustomItem(itemStack);
        if (customItem != null) {
            if (!ProtectionUtil.canBuild(location, e.getPlayer())) return;
            if (customItem instanceof CustomPlaceableItem placeableItem) {
                boolean placed = CustomBlockManager.attemptPlacement(e.getItem(), e.getPlayer(), placeableItem, location, e.getBlockFace(), true);
                if (placed) {
                    e.setCancelled(true);
                    switch (e.getHand()) {
                        case HAND -> e.getPlayer().swingMainHand();
                        case OFF_HAND -> e.getPlayer().swingOffHand();
                    }

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
    }

    @EventHandler
    public void onHitBarrier(PlayerInteractEvent e) {
        if (e.isCancelled()) return;
        if (e.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;

        if (e.getClickedBlock().getType() == Material.BARRIER) {
            if (!ProtectionUtil.canBuild(e.getClickedBlock().getLocation(), e.getPlayer())) return;
            CustomBlockManager.breakBlock(e.getPlayer(), e.getClickedBlock().getLocation());
        }
    }

    private Location getLocationFromBlockFace(Location location, BlockFace blockFace) {
        Location temp = location;
        // todo uhm why not use Block#getRelative ?
        switch (blockFace) {
            case UP -> temp.add(0, 1, 0);
            case WEST -> temp.add(-1, 0, 0);
            case EAST -> temp.add(1, 0, 0);
            case NORTH -> temp.add(0, 0, -1);
            case SOUTH -> temp.add(0, 0, 1);
            case DOWN -> temp.add(0, -1, 0);
        }
        return temp;
    }

    @EventHandler
    public void interactFurniture(PlayerInteractEntityEvent e) {
//        if (e.isCancelled()) return;
        ItemStack heldItem = e.getPlayer().getInventory().getItem(e.getHand());
        if (e.getPlayer().isSneaking()) return;

        if (e.getRightClicked() instanceof ItemFrame itemFrame) {
            CustomItem customItem = CustomBlockManager.getFurniture(itemFrame);
            if (customItem instanceof CustomPlaceableItem customBlockItem) {
                e.setCancelled(true);

                // dye if applicable
                if (heldItem != null && heldItem.getType().toString().toUpperCase().endsWith("DYE")) {
                    if (customItem.isDyeable() && itemFrame.getItem().getItemMeta() instanceof LeatherArmorMeta leatherMeta) {
                        leatherMeta.setColor(getColorFromDye(heldItem.getType()));
                        var item = itemFrame.getItem();
                        item.setItemMeta(leatherMeta);
                        itemFrame.setItem(item);
                        heldItem.setAmount(heldItem.getAmount() - 1);
                        return;
                    }
                }

                if (customBlockItem.getCustomBlock().isSeated()) {
                    double offset = customBlockItem.getCustomBlock().getSeatOffset();
                    GSitAPI.createSeat(itemFrame.getLocation().getBlock(), e.getPlayer(), true, 0.0, offset, 0.0, e.getPlayer().getLocation().getYaw(), true);
                }
                else {
//                    if (!WorldGuardUtil.canBuildInRegion(e.getPlayer())) return;
                    Bukkit.getPluginManager().callEvent(new FurnitureInteractEvent(e.getPlayer(), customItem, itemFrame, e.getHand(), heldItem));
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJump(PlayerMoveEvent e) {
        if (e.getPlayer().isSneaking()) return;

        Player player = e.getPlayer();
        double velocity = Math.round(player.getVelocity().getY()*100d) / 100d;


        // player has jump velocity
        if (velocity >= 0.42) {
            if (!isStandingOnEnderPad(e.getPlayer())) return;

            CompletableFuture.supplyAsync(() -> {
                for (int i = 2; i <= 30; i++) {
                    Block block = e.getPlayer().getWorld().getBlockAt(e.getPlayer().getLocation().add(0, i, 0));
                    if (block.getType() == Material.BARRIER) {
                        return block;
                    }
                }
                return null;
            }).thenAccept(ender_pad -> runTeleportation(player, ender_pad));
        }
    }

    private void runTeleportation(Player player, Block ender_pad) {
        Bukkit.getScheduler().runTask(GardensFurniture.plugin, () -> {
            if (ender_pad != null) {
                ItemDisplay itemDisplay = CustomBlockManager.getDisplayEntity(ender_pad.getLocation());
                if (itemDisplay == null || itemDisplay.getItemStack() == null) return;

                CustomItem customItem = GardensFurniture.itemRegistry.getCustomItem(itemDisplay.getItemStack());
                if (customItem == null) return;

                if (!customItem.getIdentifier().startsWith("ender_pad")) {
                    return;
                }
                Location loc = player.getLocation();
                loc.setY(ender_pad.getY() + 1);

                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 5);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 0.2f, 0.2f);
                player.teleport(loc);
                player.getWorld().spawnParticle(Particle.PORTAL, loc, 5);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 0.2f, 0.2f);
            }
        });
    }

    private boolean isStandingOnEnderPad(Player player) {
        Block block = player.getLocation().subtract(0, 1, 0).getBlock();
        if (block.getType() == Material.BARRIER) {
            ItemDisplay display = CustomBlockManager.getDisplayEntity(block.getLocation());
            if (display != null && display.getItemStack() != null) {
                CustomItem customItem = GardensFurniture.itemRegistry.getCustomItem(display.getItemStack());
                if (customItem != null) {
                    return customItem.getIdentifier().startsWith("ender_pad");
                }
            }
        }
        return false;
    }

    private Color getColorFromDye(Material material) {
        Color color;
        switch (material) {
            case BLACK_DYE -> color = Color.BLACK;
            case BLUE_DYE -> color = Color.BLUE;
            case BROWN_DYE -> color = Color.fromRGB(160,82,45);
            case CYAN_DYE -> color = Color.AQUA;
            case GRAY_DYE -> color = Color.GRAY;
            case GREEN_DYE -> color = Color.GREEN;
            case LIGHT_BLUE_DYE -> color = Color.fromRGB(137, 207, 240);
            case LIGHT_GRAY_DYE -> color = Color.SILVER;
            case LIME_DYE -> color = Color.LIME;
            case MAGENTA_DYE -> color = Color.FUCHSIA;
            case ORANGE_DYE -> color = Color.ORANGE;
            case PINK_DYE -> color = Color.fromRGB(255,192,203);
            case PURPLE_DYE -> color = Color.PURPLE;
            case RED_DYE -> color = Color.RED;
            case WHITE_DYE -> color = Color.WHITE;
            case YELLOW_DYE -> color = Color.YELLOW;
            default -> throw new RuntimeException("Unknown dye: " + material);
        }
        return color;
    }
}
