package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.block.CustomNoteBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomWaxedBlock;
import com.gardensmc.gardensfurniture.custom.item.*;
import com.gardensmc.gardensfurniture.custom.item.mechanics.CaptureMobHandler;
import com.gardensmc.gardensfurniture.events.CustomBlockInteractEvent;
import com.gardensmc.gardensfurniture.store.FurnitureStore;
import com.gardensmc.gardensfurniture.util.ProtectionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

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

                    if (stack.getType().toString().endsWith("AXE")) {
                        System.out.println("Axe hit");
                        var mechanics = customItem.getMechanics();
                        if (mechanics != null && mechanics.getStripped_log() != null) {
                            System.out.println("Stripped log mechanic found: " + mechanics.getStripped_log());
                            // todo helper method for noteblock replacement ? then we can just be like replace this block and it will internally do so in the fast way
                            var strippedLog = GardensFurniture.itemRegistry.getItem(mechanics.getStripped_log());
                            if (customItem instanceof CustomDirectionalItem directionalItem && strippedLog instanceof CustomDirectionalItem blockItem) {
                                System.out.println("Found directional item");
                                var direction = directionalItem.getBlockDirection(noteBlock);
                                if (direction != null) {
                                    System.out.println("Found direction: " + direction);
                                    if (blockItem.getCustomBlock(direction) instanceof CustomNoteBlock customNoteBlock) {
                                        noteBlock.setInstrument(customNoteBlock.getInstrument());
                                        noteBlock.setNote(customNoteBlock.getNote());
                                        noteBlock.setPowered(customNoteBlock.isPowered());
                                        e.getClickedBlock().setBlockData(noteBlock);
                                        e.getInteractionPoint().getWorld().playSound(
                                                e.getInteractionPoint(), Sound.ITEM_AXE_STRIP, 1, 1
                                        );

                                        // decrement axe durability
                                        if (e.getPlayer().getGameMode() != GameMode.CREATIVE && stack.getItemMeta() instanceof Damageable damageable) {
                                            damageable.setDamage(damageable.getDamage() + 2);
                                            stack.setItemMeta(damageable);
                                        }
                                        return;
                                    }
                                }
                            }
                        }
                    }

                    net.minecraft.world.item.ItemStack craftItemStack = CraftItemStack.asNMSCopy(stack);
                    ServerPlayer human = ((CraftPlayer) e.getPlayer()).getHandle();
                    Direction direction;
                    switch (e.getBlockFace()) {
                        case SOUTH -> direction = Direction.SOUTH;
                        case WEST -> direction = Direction.WEST;
                        case NORTH -> direction = Direction.NORTH;
                        case EAST -> direction = Direction.EAST;
                        case DOWN -> direction = Direction.DOWN;
                        default -> direction = Direction.UP;
                    }
                    var result = craftItemStack.useOn(new UseOnContext(human, InteractionHand.MAIN_HAND, new BlockHitResult(
                            new Vec3(e.getInteractionPoint().getX(), e.getInteractionPoint().getY(), e.getInteractionPoint().getZ()),
                            direction,
                            new BlockPos(e.getClickedBlock().getX(), e.getClickedBlock().getY(), e.getClickedBlock().getZ()),
                            false
                    )));
                    if (result != InteractionResult.FAIL) {
                        // TODO do for other packet types
                        // manually create furniture stores
                        if (customItem instanceof CustomPlaceableItem placeable && placeable.getCustomBlock() instanceof CustomWaxedBlock customWaxed) {
                            customWaxed.placeBlock(e.getPlayer(), e.getItem(), placeable, e.getClickedBlock().getRelative(e.getBlockFace()).getLocation(), new PlaceOptions().setBlockFace(e.getBlockFace()));
                        }
                    }
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
                boolean placed = CustomBlockManager.attemptPlacement(e.getPlayer(), e.getItem(), placeableItem, location, new PlaceOptions().setBlockFace(e.getBlockFace()));
                if (placed) {
                    e.setCancelled(true);
                    switch (e.getHand()) {
                        case HAND -> e.getPlayer().swingMainHand();
                        case OFF_HAND -> e.getPlayer().swingOffHand();
                    }

                    if (e.getPlayer().getGameMode() == GameMode.SURVIVAL) {
                        ItemStack usedItem = e.getPlayer().getInventory().getItem(e.getHand());
                        usedItem.setAmount(usedItem.getAmount() - 1);
                        e.getPlayer().getInventory().setItem(e.getHand(), usedItem);
                    }
                }
            } else if (customItem.getMechanics() != null && customItem.getMechanics().isCaptureMobs()) {
                var captureMobHandler = new CaptureMobHandler(e.getPlayer(), new WrappedItem(itemStack, customItem));
                if (captureMobHandler.isStoringMob()) {
                    captureMobHandler.releaseMob(location);
                    e.setCancelled(true);
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
            CustomBlockManager.breakFurniture(e.getPlayer(), e.getClickedBlock().getLocation());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRightClickBarrier(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getHand() == EquipmentSlot.HAND) { // called for each hand
            var block = e.getClickedBlock();
            if (block != null && block.getType() == Material.BARRIER) {
                if (!ProtectionUtil.canBuild(block.getLocation(), e.getPlayer())) return;
                var itemEntity = CustomBlockManager.getItemEntity(block.getLocation());
                if (itemEntity == null) return;
                CustomItem customItem = GardensFurniture.itemRegistry.getCustomItem(itemEntity.getItem());
                Bukkit.getPluginManager().callEvent(new CustomBlockInteractEvent(
                        e.getPlayer(), customItem, e.getHand(), e.getItem(), block
                ));
            }
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

    @EventHandler(ignoreCancelled = true)
    public void interactWithDestroyStick(PlayerInteractEntityEvent e) {
        ItemStack heldItem = e.getPlayer().getInventory().getItem(e.getHand());
        var customItem = GardensFurniture.itemRegistry.getCustomItem(heldItem);
        if (customItem == null) return;
        if (customItem.getIdentifier().equalsIgnoreCase("destroy_stick") && e.getPlayer().hasPermission("gardens.destroy_stick")) {
            e.getRightClicked().remove();
        }
    }

    @EventHandler
    public void interactFurniture(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof ItemFrame || e.getRightClicked() instanceof Interaction) {
            var itemEntity = CustomBlockManager.getItemEntity(e.getRightClicked().getLocation());
            if (itemEntity == null) return;
            CustomBlockManager.onInteractFurniture(e.getPlayer(), e.getHand(), itemEntity);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void interactMobForCapture(PlayerInteractEntityEvent e) {
        var mob = e.getRightClicked();
        if (!(mob instanceof LivingEntity livingEntity) || CaptureMobHandler.CAPTURABLE_MOB_FILTER.contains(mob.getType())) {
            return;
        }
        if (!ProtectionUtil.canBuild(mob.getLocation(), e.getPlayer())) {
            return;
        }
        var wrappedItem = WrappedItem.create(e.getPlayer().getInventory().getItem(e.getHand()));
        if (wrappedItem.isPresent()) {
            var captureMobHandler = new CaptureMobHandler(e.getPlayer(), wrappedItem.get());
            if (captureMobHandler.isCaptureItem() && captureMobHandler.canCaptureMob(livingEntity)) {
                captureMobHandler.captureMob(livingEntity);
            }
        }
    }

    @EventHandler
    public void onInteractBarrier(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        var block = e.getClickedBlock();
        if (block != null && block.getType() == Material.BARRIER) {
            var itemEntity = CustomBlockManager.getItemEntity(block.getLocation());
            if (itemEntity == null) return;
            CustomBlockManager.onInteractFurniture(e.getPlayer(), e.getHand(), itemEntity);
            e.setCancelled(true);
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


}
