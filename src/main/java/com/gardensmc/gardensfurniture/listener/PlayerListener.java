package com.gardensmc.gardensfurniture.listener;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.block.CustomNoteBlock;
import com.gardensmc.gardensfurniture.custom.block.CustomWaxedBlock;
import com.gardensmc.gardensfurniture.custom.item.*;
import com.gardensmc.gardensfurniture.custom.item.mechanics.CaptureMobHandler;
import com.gardensmc.gardensfurniture.events.CustomBlockInteractEvent;
import com.gardensmc.gardensfurniture.menu.DyeMenu;
import com.gardensmc.gardensfurniture.store.FurnitureStoreHandler;
import com.gardensmc.gardensfurniture.structure.StructureHandler;
import com.gardensmc.gardensfurniture.util.ProbabilityUtil;
import com.gardensmc.gardensfurniture.util.ProtectionUtil;
import de.tr7zw.nbtapi.NBT;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.block.data.type.Slab;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static com.gardensmc.gardensfurniture.listener.BlockListener.handleFeature;

public class PlayerListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onCustomItemInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.isCancelled()) return;
        if (e.getClickedBlock() == null) return;

        ItemStack itemStack = e.getItem();
        if (itemStack != null && e.getHand() != null) {
            // handle placement of custom items
            var blockLoc = CustomBlockManager.isPlaceableMaterial(e.getClickedBlock().getType())
                    ? e.getClickedBlock()
                    : e.getClickedBlock().getRelative(e.getBlockFace());
            if (!CustomBlockManager.isPlaceableMaterial(blockLoc.getType())) return;
            var location = blockLoc.getLocation();

            if (!e.getPlayer().isSneaking() && isInteractableBlock(e.getClickedBlock())) {
                return;
            }
            CustomItem customItem = GardensFurniture.itemRegistry.getCustomItem(itemStack);
            if (customItem != null) {
                if (ProtectionUtil.canBuild(location, e.getPlayer())) {
                    if (customItem instanceof CustomPlaceableItem placeableItem) {
                        // adventure mode can't place blocks in vanilla either
                        if (e.getPlayer().getGameMode() == GameMode.ADVENTURE) return;
                        boolean placed = CustomBlockManager.attemptPlacement(e.getPlayer(), e.getItem(), placeableItem, location, new PlaceOptions().setBlockFace(e.getBlockFace()), e.getClickedBlock());
                        if (placed) {
                            e.setCancelled(true);
                            switch (e.getHand()) {
                                case HAND -> e.getPlayer().swingMainHand();
                                case OFF_HAND -> e.getPlayer().swingOffHand();
                            }

                            if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
                                ItemStack usedItem = e.getPlayer().getInventory().getItem(e.getHand());
                                usedItem.setAmount(usedItem.getAmount() - 1);
                                e.getPlayer().getInventory().setItem(e.getHand(), usedItem);
                            }
                        }
                    } else if (customItem.getMechanics() != null && customItem.getMechanics().isCaptureMobs()) {
                        var captureMobHandler = new CaptureMobHandler(e.getPlayer(), new WrappedItem(itemStack, customItem), e.getHand());
                        if (captureMobHandler.isStoringMob()) {
                            captureMobHandler.releaseMob(location);
                            e.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    // Combining a waxed slab into a double: Bedrock doesn't do client-side slab-merge prediction for
    // custom blocks, so it draws a ghost full slab in the cell it clicked toward while the server
    // merges it into a double one block over. The server never changes that cell, so Geyser never
    // corrects it. Resend the affected cells to the placing player to discard the ghost (Geyser only;
    // Java gets a harmless redundant update).
    // NOT ignoreCancelled: the placement handler above cancels the event on a successful place, and
    // we still need to fire to clear the Bedrock ghost.
    @EventHandler
    public void onWaxedSlabCombineRefresh(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null || e.getItem() == null) {
            return;
        }
        if (!(e.getClickedBlock().getBlockData() instanceof org.bukkit.block.data.type.Slab)) {
            return;
        }
        var customItem = GardensFurniture.itemRegistry.getCustomItem(e.getItem());
        if (!(customItem instanceof CustomBlockItem blockItem)
                || !(blockItem.getCustomBlock() instanceof CustomWaxedBlock waxedBlock)
                || !waxedBlock.getWaxedType().toString().startsWith("SLAB")) {
            return;
        }
        var player = e.getPlayer();
        var clicked = e.getClickedBlock();
        var face = e.getBlockFace();
        // resend at a few delays so we catch the client prediction whenever it settles
        for (long delay : new long[]{1L, 3L, 6L}) {
            Bukkit.getScheduler().runTaskLater(GardensFurniture.plugin, () -> {
                for (BlockFace f : new BlockFace[]{BlockFace.SELF, BlockFace.UP, BlockFace.DOWN, face}) {
                    var b = clicked.getRelative(f);
                    player.sendBlockChange(b.getLocation(), b.getBlockData());
                }
            }, delay);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBoneMealTripwire(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        var heldItem = e.getItem();
        if (heldItem == null || heldItem.getType() != Material.BONE_MEAL || e.getHand() == null) {
            return;
        }
        var block = e.getClickedBlock();
        if (block != null && block.getType() == Material.TRIPWIRE) {
            var customItem = CustomBlockManager.getItemFromLeaves(e.getClickedBlock());
            if (customItem == null || customItem.getMechanics() == null) {
                return;
            }
            var fungusStructure = customItem.getMechanics().getFungus_structure();
            if (fungusStructure != null && ProbabilityUtil.roll(40)) {
                if (!StructureHandler.INSTANCE.hasStructure(fungusStructure)
                        && ProtectionUtil.hasForeignClaimNear(block.getLocation(), StructureHandler.FEATURE_CLAIM_RADIUS)) {
                    return; // datapack features can't be limited per block
                }
                CustomBlockManager.removeCustomBlock(block);
                // deplete bone meal by 1
                heldItem.setAmount(heldItem.getAmount() - 1);
                e.getPlayer().getInventory().setItem(e.getHand(), heldItem);

                if (!StructureHandler.INSTANCE.spawnGrownStructure(fungusStructure, block.getLocation())) {
                    try {
                        handleFeature(fungusStructure, block.getLocation());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onNoteBlockInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;

        // cancel all note block right clicks that are custom blocks
        if (e.getClickedBlock().getType() == Material.NOTE_BLOCK && e.getClickedBlock().getBlockData() instanceof NoteBlock noteBlock) {
            CustomItem customItem = CustomBlockManager.getItemFromLeaves(e.getClickedBlock());
            if (customItem != null) {
                var event = new CustomBlockInteractEvent(e.getPlayer(), customItem, e.getHand(), e.getItem(), e.getClickedBlock(), e.getBlockFace());
                Bukkit.getPluginManager().callEvent(event);

                // always suppress the vanilla note block interaction (retune) and placement for custom blocks
                e.setCancelled(true);

                // a listener fully handled this interaction (e.g. placed its own block); skip our default placement
                if (event.isCancelled()) {
                    return;
                }

                if (customItem.getIdentifier().equals("paint_bench")) {
                    if (ProtectionUtil.hasClaimPermission(e.getClickedBlock().getLocation(), e.getPlayer(), ClaimPermission.Access)) {
                        new DyeMenu(e.getPlayer()).open();
                    } else {
                        e.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<red>You do not have permission to use that here"));
                    }
                    return;
                }

                if (!ProtectionUtil.canBuild(e.getClickedBlock().getLocation(), e.getPlayer())) {
                    return;
                }

                // if player is holding an item with an action, lets place it
                if (e.getHand() != null) {
                    ItemStack stack = e.getPlayer().getInventory().getItem(e.getHand());
                    if (stack == null || stack.getType() == Material.AIR || stack.getType() == Material.BONE_MEAL) return;
                    // like vanilla, the offhand is only used when the main hand has nothing to use
                    if (e.getHand() == EquipmentSlot.OFF_HAND && !e.getPlayer().getInventory().getItemInMainHand().getType().isAir()) return;

                    if (stack.getType().toString().endsWith("AXE")) {
                        var mechanics = customItem.getMechanics();
                        if (mechanics != null && mechanics.getStripped_log() != null) {
                            var strippedLog = GardensFurniture.itemRegistry.getItem(mechanics.getStripped_log());
                            if (customItem instanceof CustomDirectionalItem directionalItem && strippedLog instanceof CustomDirectionalItem blockItem) {
                                var direction = directionalItem.getBlockDirection(e.getClickedBlock());
                                if (direction != null) {
                                    if (blockItem.getCustomBlock(direction) instanceof CustomNoteBlock customNoteBlock) {
                                        e.getClickedBlock().setType(Material.AIR);
                                        customNoteBlock.placeBlock(null, stack, blockItem, e.getClickedBlock().getLocation(), null);
                                        e.getInteractionPoint().getWorld().playSound(
                                                e.getInteractionPoint(), Sound.ITEM_AXE_STRIP, 1, 1
                                        );

                                        // decrement axe durability
                                        var damageEvent = new PlayerItemDamageEvent(e.getPlayer(), stack, 2, 2);
                                        Bukkit.getPluginManager().callEvent(damageEvent);
                                        if (!damageEvent.isCancelled() && e.getPlayer().getGameMode() != GameMode.CREATIVE && stack.getItemMeta() instanceof Damageable damageable) {
                                            damageable.setDamage(damageable.getDamage() + damageEvent.getDamage());
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
                    // the context consumes from this hand, so it must be the hand the item came from
                    var hand = e.getHand() == EquipmentSlot.OFF_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                    var result = craftItemStack.useOn(new UseOnContext(human, hand, new BlockHitResult(
                            new Vec3(e.getInteractionPoint().getX(), e.getInteractionPoint().getY(), e.getInteractionPoint().getZ()),
                            direction,
                            new BlockPos(e.getClickedBlock().getX(), e.getClickedBlock().getY(), e.getClickedBlock().getZ()),
                            false
                    )));
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null || e.getClickedBlock().getType() != Material.FLOWER_POT) return;

        if (CustomBlockManager.getPottedPlant(e.getClickedBlock()) != null) {
            e.setCancelled(true); // cancel placement of normal flower pot if our custom is planted already
            return;
        }

        var heldItem = GardensFurniture.itemRegistry.getCustomItem(e.getItem());
        if (heldItem == null) return;
        var mechanics = heldItem.getMechanics();
        if (mechanics == null || mechanics.getPot_item() == null) return;

        if (!ProtectionUtil.canBuild(e.getClickedBlock().getLocation(), e.getPlayer())) return;
        var potItem = GardensFurniture.itemRegistry.getItem(mechanics.getPot_item());
        if (potItem == null) {
            throw new RuntimeException("Gurl you need to register an actual item for the pot item called: " + mechanics.getPot_item());
        }
        // save item to store
        var itemToStore = e.getItem().clone();
        itemToStore.setAmount(1);
        // deplete held amount
        if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            e.getItem().setAmount(e.getItem().getAmount() - 1);
        }
        // plant
        e.setCancelled(true); // if held item is also a real plant (ie sapling) it'll plant both, so lets cancel
        ItemDisplay display = (ItemDisplay) e.getClickedBlock().getWorld().spawnEntity(e.getClickedBlock().getLocation().clone().add(0.5, 0, 0.5), EntityType.ITEM_DISPLAY);
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        display.setItemStack(GardensFurniture.itemRegistry.buildItem(potItem));
//        display.setBrightness(new Display.Brightness(0, 15));
        display.setTransformation(new Transformation(
                new Vector3f(0, 0.65f, 0), // translate up to sit on pot nicely
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(0.75f, 0.75f, 0.75f), // scale down
                new AxisAngle4f(0, 0, 0, 1)
        ));
        NBT.modifyPersistentData(display, nbt -> {
            nbt.setItemStack(CustomBlockManager.POTTED_KEY, itemToStore);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() != Material.FLOWER_POT) return;
        var plantedItem = CustomBlockManager.getPottedPlant(e.getBlock());
        if (plantedItem == null) return;
        var plantedHeldItem = NBT.getPersistentData(plantedItem, nbt -> nbt.getItemStack(CustomBlockManager.POTTED_KEY));
        if (plantedHeldItem == null) return;

        plantedItem.remove();
        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), plantedHeldItem);
    }

    @EventHandler(ignoreCancelled = true)
    public void onRemoveWaxed(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;
        if (e.getItem() == null || !e.getItem().getType().toString().toLowerCase().endsWith("axe")) return;
        if (CustomWaxedBlock.WaxedType.materials().contains(e.getClickedBlock().getType())) {
            if (CustomBlockManager.getCustomItemFromBlock(e.getClickedBlock()) != null) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK &&
                e.getClickedBlock() != null &&
                e.getClickedBlock().getType().name().toUpperCase().endsWith("SHULKER_BOX")) {

            // this fires BEFORE the inventory opens
            // delay 1 tick so server finishes its updates
            Bukkit.getScheduler().runTaskLater(GardensFurniture.plugin, () -> getNearbyPacketFurniture(
                    e.getClickedBlock()).forEach(block -> e.getPlayer().sendBlockChange(block.getLocation(), block.getBlockData())), 1L);
        }
    }

    private List<Block> getNearbyPacketFurniture(Block shulkerBlock) {
        return IntStream.range(-5, 6).mapToObj(i -> shulkerBlock.getRelative(0, i, 0))
                .filter(block -> FurnitureStoreHandler.INSTANCE.getFurnitureStore(block) != null)
                .toList();
    }

    @EventHandler
    public void onHitBarrier(PlayerInteractEvent e) {
        if (e.isCancelled()) return;
        if (e.getAction() != Action.LEFT_CLICK_BLOCK) return;
        var block = e.getClickedBlock();
        if (block == null || block.getType() != Material.BARRIER) return;
        if (e.getPlayer().getInventory().getItem(e.getHand() == null ? EquipmentSlot.HAND : e.getHand()).getType().toString().toLowerCase().endsWith("sword")) return;
        if (!ProtectionUtil.canBuild(block.getLocation(), e.getPlayer())) return;

        // In survival, barrier furniture that defines material_traits breaks gradually via the
        // CustomBreakingBlock system (BlockDamageEvent). Insta-break here only when it has no defined
        // break speed, or in creative - where digging is instant anyway and the gradual handler bails out.
        if (e.getPlayer().getGameMode() != GameMode.CREATIVE && hasMaterialTraits(block)) {
            return;
        }
        CustomBlockManager.breakFurniture(e.getPlayer(), block.getLocation(), false);
    }

    private boolean hasMaterialTraits(Block block) {
        return CustomBlockManager.getCustomItemFromBlock(block) instanceof CustomPlaceableItem placeable
                && placeable.getCustomBlock().getMaterialTraits() != null;
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
            var clicked = e.getRightClicked();
            // furniture is a display plus hitbox entities/barriers: remove the whole piece (no drop), not
            // just the part that was clicked, which would leave the rest orphaned
            if ((clicked instanceof Interaction || clicked instanceof ItemDisplay)
                    && CustomBlockManager.getFurnitureEntity(clicked.getLocation()) != null) {
                CustomBlockManager.breakFurniture(null, clicked.getLocation(), false);
            } else {
                clicked.remove();
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void interactFurniture(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof Interaction interaction) {
            var itemEntity = CustomBlockManager.getFurnitureEntity(e.getRightClicked().getLocation());
            if (itemEntity == null) return;
            if (GardensFurniture.itemRegistry.getCustomItem(itemEntity.getItem()) == null) {
                return;
            }
            CustomBlockManager.onInteractFurniture(e.getPlayer(), e.getHand(), itemEntity, interaction);
            e.setCancelled(true);
        }
    }

    // Captured mobs live in real spawn eggs. Vanilla would spend one on a spawner, on water, on a mob
    // of the same type (spawning a baby) or from a dispenser, losing the stored mob. Our release (in
    // onCustomItemInteract) spawns the mob itself, so vanilla's own egg use can always be denied.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void denyVanillaCapturedEggUse(PlayerInteractEvent e) {
        if (CaptureMobHandler.isCapturedMob(e.getItem())) {
            e.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void denyCapturedEggOnMob(PlayerInteractEntityEvent e) {
        if (CaptureMobHandler.isCapturedMob(e.getPlayer().getInventory().getItem(e.getHand()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void denyCapturedEggDispense(BlockDispenseEvent e) {
        if (CaptureMobHandler.isCapturedMob(e.getItem())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void interactMobForCapture(PlayerInteractEntityEvent e) {
        // PlayerInteractEntityEvent fires once per hand; only handle the main hand
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        var mob = e.getRightClicked();
        if (!(mob instanceof LivingEntity livingEntity) || CaptureMobHandler.CAPTURABLE_MOB_FILTER.contains(mob.getType())) {
            return;
        }
        if (!ProtectionUtil.canBuild(mob.getLocation(), e.getPlayer())) {
            return;
        }
        var wrappedItem = WrappedItem.create(e.getPlayer().getInventory().getItem(e.getHand()));
        if (wrappedItem.isPresent()) {
            var captureMobHandler = new CaptureMobHandler(e.getPlayer(), wrappedItem.get(), e.getHand());
            if (captureMobHandler.isCaptureItem() && captureMobHandler.canCaptureMob(livingEntity)) {
                captureMobHandler.captureMob(livingEntity);
                // cancel so vanilla doesn't follow through and use the resulting spawn egg
                // on the block/fluid behind the mob (e.g. re-spawning it when standing in water)
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onInteractBarrier(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        // sneaking right-clicks go to vanilla, like on any other block (e.g. placing a block against a table)
        if (e.getPlayer().isSneaking()) return;
        var block = e.getClickedBlock();
        if (block != null && block.getType() == Material.BARRIER) {
            var itemEntity = CustomBlockManager.getFurnitureEntity(block.getLocation());
            if (itemEntity == null) return;
            CustomBlockManager.onInteractFurniture(e.getPlayer(), e.getHand(), itemEntity, null);
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJump(PlayerJumpEvent e) {
        if (e.getPlayer().isSneaking()) return;
        if (!isStandingOnEnderPad(e.getPlayer())) return;

        runTeleportation(e.getPlayer(), findEnderPadBarrier(e.getPlayer(), 1));
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return;
        if (!isStandingOnEnderPad(e.getPlayer())) return;

        runTeleportation(e.getPlayer(), findEnderPadBarrier(e.getPlayer(), -1));
    }

    private static final Set<Material> INTERACTABLES = Set.of(
            Material.ANVIL,
            Material.ENCHANTING_TABLE,
            Material.CRAFTING_TABLE,
            Material.ENDER_CHEST,
            Material.CARTOGRAPHY_TABLE,
            Material.GRINDSTONE,
            Material.SMITHING_TABLE,
            Material.BEACON,
            Material.LOOM
    );

    private boolean isInteractableBlock(Block block) {
        if (block.getBlockData() instanceof Openable || block.getState() instanceof InventoryHolder) {
            return true;
        }
        var customItem = CustomBlockManager.getItemFromLeaves(block);
        if (customItem != null && customItem.getIdentifier().equals("paint_bench")) {
            return true;
        }
        return INTERACTABLES.contains(block.getType()) || block.getType().toString().toLowerCase().endsWith("bed");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player player && e.getFinalDamage() >= player.getHealth()) {
            // close inventory before death to allow onClose to run
            player.getOpenInventory().close();
        }
    }

    // world access has to stay on the main thread; a 30 block column check is cheap enough
    @Nullable
    private Block findEnderPadBarrier(Player player, int direction) {
        for (int i = 2; i <= 30; i++) {
            Block block = player.getWorld().getBlockAt(player.getLocation().add(0, i * direction, 0));
            if (block.getType() == Material.BARRIER) {
                return block;
            }
        }
        return null;
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
