package com.gardensmc.gardensfurniture.events;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.gardensmc.gardensfurniture.custom.CustomBlockManager;
import com.gardensmc.gardensfurniture.custom.CustomItem;
import com.gardensmc.gardensfurniture.leaves.LeaveWorld;
import com.gardensmc.gardensfurniture.register.CustomRegister;
import com.gardensmc.gardensfurniture.util.LeavesConfig;
import com.gardensmc.gardensfurniture.util.WorldGuardUtil;
import dev.geco.gsit.api.GSitAPI;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class Events implements Listener {

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
                if (!WorldGuardUtil.canBuildInRegion(e.getPlayer())) return;
                Bukkit.getPluginManager().callEvent(new CustomBlockInteractEvent(e.getPlayer(), customItem, e.getHand(), e.getItem(), e.getClickedBlock()));
                // if player is holding an item with an action, lets place it if sneaking
//                if (e.getHand() != null && e.getPlayer().isSneaking()) {
//                    ItemStack stack = e.getPlayer().getInventory().getItem(e.getHand());
//                    if (stack == null || stack.getType() == Material.AIR) return;
//                    net.minecraft.world.item.ItemStack craftItemStack = CraftItemStack.asNMSCopy(stack);
//                    EntityPlayer human = ((CraftPlayer) e.getPlayer()).getHandle();
//                    EnumHand hand = EnumHand.a; // main hand
//                    EnumDirection direction;
//                    switch (e.getBlockFace()) {
//                        case SOUTH -> direction = EnumDirection.d;
//                        case WEST -> direction = EnumDirection.e;
//                        case NORTH -> direction = EnumDirection.c;
//                        case EAST -> direction = EnumDirection.f;
//                        case DOWN -> direction = EnumDirection.a;
//                        default -> direction = EnumDirection.b;
//                    }
//
//                    craftItemStack.useOn(new ItemActionContext(human, hand, new MovingObjectPositionBlock(
//                            new Vec3D(e.getClickedBlock().getX(), e.getClickedBlock().getY(), e.getClickedBlock().getZ()),
//                            direction,
//                            new BlockPosition(e.getClickedBlock().getX(), e.getClickedBlock().getY(), e.getClickedBlock().getZ()),
//                            false
//                    )), hand);
//


//                }
            }
        }

        ItemStack itemStack = e.getItem();
        if (itemStack == null) return;
        if (e.getHand() == null) return;

        // handle placement of custom items
        Location location = getLocationFromBlockFace(e.getClickedBlock().getLocation(), e.getBlockFace());
        if (location.getBlock().getType() != Material.AIR) return;

        CustomItem customItem = CustomRegister.INSTANCE.itemRegistry.getCustomItem(itemStack);
        if (customItem != null) {
            if (!WorldGuardUtil.canBuildInRegion(e.getPlayer())) return;
            if (customItem.isCustomBlock()) {
                boolean placed = CustomBlockManager.attemptPlacement(e.getPlayer(), customItem, location, e.getBlockFace());
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
            if (!WorldGuardUtil.canBuildInRegion(e.getPlayer())) return;
            CustomBlockManager.breakBlock(e.getPlayer(), e.getClickedBlock().getLocation());
        }
    }

    private Location getLocationFromBlockFace(Location location, BlockFace blockFace) {
        Location temp = location;
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
    public void playNoteBlockNote(NotePlayEvent e) {
//        System.out.println("Playing: " + e.getNote().getId());

        if (e.isCancelled()) return;
        if (e.getInstrument() != Instrument.PIANO) {
            e.setCancelled(true);
        }


//        CustomItem customItem = CustomBlockManager.getItemFromNoteBlock((NoteBlock) e.getBlock().getBlockData());
//        System.out.println("1");
//        if (customItem != null) {
//            System.out.println("2");
//
//            e.setCancelled(true);
//        }
    }



//    @EventHandler
//    public void entityBreakItemFrame(HangingBreakByEntityEvent e) {
//        if (e.isCancelled()) return;
//
//        if (e.getEntity() instanceof ItemFrame itemFrame) {
//            ItemStack itemStack = itemFrame.getItem();
//            if (itemStack.getType() != Material.AIR) {
//                CustomItem customItem = CustomRegister.INSTANCE.itemRegistry.getCustomItem(itemStack);
//                if (customItem != null) {
//                    e.setCancelled(true);
//
//                    itemFrame.getWorld().dropItemNaturally(itemFrame.getLocation(), itemStack);
//                    for (Entity entity : e.getEntity().getNearbyEntities(0, 0, 0)) {
//                        if (entity instanceof ItemFrame) {
//                            System.out.println("Removing item frame");
//                            entity.remove();
//                        }
//                    }
//                }
//            }
//        }
//    }

    @EventHandler
    public void breakItemFrame(HangingBreakEvent e) {
        if (e.isCancelled()) return;

        // prevent non-entities from breaking
        if (e.getCause() != HangingBreakEvent.RemoveCause.ENTITY) {
            if (e.getEntity() instanceof ItemFrame itemFrame) {
                ItemStack itemStack = itemFrame.getItem();
                if (itemStack.getType() != Material.AIR) {
                    CustomItem customItem = CustomRegister.INSTANCE.itemRegistry.getCustomItem(itemStack);
                    if (customItem != null) {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void entityBreakItemFrame(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) return;

        if (e.getEntity() instanceof ItemFrame itemFrame) {
            if ( e.getDamager() instanceof Player player) {

                ItemStack itemStack = itemFrame.getItem();
                if (itemStack.getType() != Material.AIR) {
                    CustomItem customItem = CustomRegister.INSTANCE.itemRegistry.getCustomItem(itemStack);
                    if (customItem != null && customItem.isCustomBlock()) {
                        e.setCancelled(true);

                        if (!WorldGuardUtil.canBuildInRegion(player)) return;
                        itemFrame.remove();
                        if (!customItem.getCustomBlock().cancelDrop()) {
                            itemFrame.getWorld().dropItemNaturally(itemFrame.getLocation(), itemStack);
                        }
                        Bukkit.getPluginManager().callEvent(new FurnitureBreakEvent(player, customItem, itemFrame));

                    }
                }
            }
            else {
                e.setCancelled(true);
            }

        }
    }

    @EventHandler
    public void placeNoteBlock(BlockPlaceEvent e) {
        if (e.isCancelled()) return;

        if (e.getBlockPlaced().getType() == Material.NOTE_BLOCK) {
            e.getBlockPlaced().setType(Material.NOTE_BLOCK, false);
//            CustomItem customItem = CustomBlockManager.getItemFromNoteBlock((NoteBlock) e.getBlockPlaced().getBlockData());
//            if (customItem == null) {
//
//            }
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (cancelNoteBlockExtend(e.getBlocks())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (cancelNoteBlockExtend(e.getBlocks())) {
            e.setCancelled(true);
        }
    }

    private boolean cancelNoteBlockExtend(List<Block> blocks) {
        for (Block block : blocks) {
            if (block.getType() == Material.NOTE_BLOCK && block.getBlockData() instanceof NoteBlock noteBlock) {
                CustomItem customItem = CustomBlockManager.getItemFromNoteBlock(noteBlock);
                if (customItem != null) {
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler
    public void updateBlockState(BlockPhysicsEvent e) {

        if (e.isCancelled()) return;
        Block[] possibles = new Block[]{
                e.getBlock(), e.getSourceBlock(), e.getSourceBlock().getRelative(0, 1, 0)
        };

        for (Block block : possibles) {
            String[] redstone_types = new String[]{
                    "lever", "redstone_torch", "redstone_wall_torch", "redstone_block", "redstone_wire"
            };
            if (Arrays.asList(redstone_types).contains(e.getChangedType().name().toLowerCase()) && block.getType() == Material.NOTE_BLOCK) {
                CustomItem customItem = CustomBlockManager.getItemFromNoteBlock((NoteBlock) block.getBlockData());
                if (customItem != null) {
                    e.setCancelled(true);
                }
            }
            else if (block.getType() == Material.NOTE_BLOCK) {
                e.setCancelled(true);
            }
        }

    }


    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {

        if (e.isCancelled()) return;

        if (e.getBlock().getType() == Material.NOTE_BLOCK && e.getBlock().getBlockData() instanceof NoteBlock noteBlock) {
            CustomItem customItem = CustomBlockManager.getItemFromNoteBlock(noteBlock);
            dropCustomItem(e, customItem);
        }
        else if (e.getBlock().getType() == Material.SPRUCE_LEAVES) {
            CustomItem customItem = CustomBlockManager.getItemFromLeaves(e.getBlock());
            dropCustomItem(e, customItem);
        }
    }

    private void dropCustomItem(BlockBreakEvent e, CustomItem customItem) {
        if (customItem != null) {
            e.setDropItems(false);

            if (e.getPlayer().getGameMode() != GameMode.CREATIVE && !customItem.getCustomBlock().cancelDrop()){
                ItemStack itemStack = CustomRegister.INSTANCE.itemRegistry.buildItem(customItem);
                if (itemStack != null) {
                    e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), itemStack);
                }
            }

            Bukkit.getPluginManager().callEvent(new CustomBlockBreakEvent(e.getPlayer(), customItem, e.getBlock()));
            LeavesConfig.removeLeave(e.getBlock());
        }
    }

    @EventHandler
    public void interactFurniture(PlayerInteractEntityEvent e) {
//        if (e.isCancelled()) return;
        ItemStack heldItem = e.getPlayer().getInventory().getItem(e.getHand());
        if (e.getPlayer().isSneaking()) return;

        if (e.getRightClicked() instanceof ItemFrame itemFrame) {
            CustomItem customItem = CustomBlockManager.getFurniture(itemFrame);
            if (customItem != null && customItem.isCustomBlock()) {

                e.setCancelled(true);

                if (customItem.getCustomBlock().isSeated()) {
                    double offset = customItem.getCustomBlock().getSeatOffset();
                    GSitAPI.createSeat(itemFrame.getLocation().getBlock(), e.getPlayer(), true, 0.0, offset, 0.0, e.getPlayer().getLocation().getYaw(), true);
                }
                else {
//                    if (!WorldGuardUtil.canBuildInRegion(e.getPlayer())) return;
                    Bukkit.getPluginManager().callEvent(new FurnitureInteractEvent(e.getPlayer(), customItem, itemFrame, e.getHand(), heldItem));
                }
            }
        }
    }

    @EventHandler
    public void onLoadChunk(ChunkLoadEvent e) {
        LeaveWorld leaveWorld = GardensFurniture.getLeaveWorld(e.getWorld());
        leaveWorld.loadChunk(e.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        LeaveWorld leaveWorld = GardensFurniture.getLeaveWorld(e.getWorld());
        leaveWorld.removeChunk(e.getChunk());
    }

//    @EventHandler
//    public void onLeaveDecay(LeavesDecayEvent e) {
//        if (e.isCancelled()) return;
//
//        LeaveWorld leaveWorld = GardensFurniture.getLeaveWorld(e.getBlock().getWorld());
//        String leaveId = leaveWorld.getLeaveId(e.getBlock());
//        if (leaveId != null) {
//            e.setCancelled(true);
//        }
//    }

}
