//package com.gardensmc.gardensfurniture.listener.sounds;
//
//import com.comphenix.protocol.PacketType;
//import com.comphenix.protocol.events.ListenerPriority;
//import com.comphenix.protocol.events.PacketAdapter;
//import com.comphenix.protocol.events.PacketContainer;
//import com.comphenix.protocol.events.PacketEvent;
//import com.comphenix.protocol.wrappers.BlockPosition;
//import com.comphenix.protocol.wrappers.EnumWrappers;
//import com.gardensmc.gardensfurniture.GardensFurniture;
//import org.bukkit.Bukkit;
//import org.bukkit.GameMode;
//import org.bukkit.Location;
//import org.bukkit.SoundCategory;
//import org.bukkit.block.Block;
//import org.bukkit.scheduler.BukkitScheduler;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static com.gardensmc.gardensfurniture.listener.sounds.VanillaBlockSoundListener.WOOD_SOUND_GROUP;
//
//public class DigSoundListener {
//    private final Map<Location, BukkitScheduler> blockBreakers = new HashMap<>();
//    private final List<Block> blockWithActiveSound = new ArrayList<>();
//
//    public static float VANILLA_HIT_VOLUME = 0.25f;
//    public static float VANILLA_HIT_PITCH = 0.5f;
//
//    private final PacketAdapter digListener = new PacketAdapter(GardensFurniture.plugin, ListenerPriority.LOW, PacketType.Play.Client.BLOCK_DIG) {
//        @Override
//        public void onPacketReceiving(PacketEvent event) {
//            if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
//
//            final PacketContainer packet = event.getPacket();
//            EnumWrappers.PlayerDigType type;
//            try {
//                type = packet.getEnumModifier(EnumWrappers.PlayerDigType.class, 2).getValues().get(0);
//            } catch (IllegalArgumentException exception) {
//                type = EnumWrappers.PlayerDigType.SWAP_HELD_ITEMS;
//            }
//            final BlockPosition pos = packet.getBlockPositionModifier().getValues().get(0);
//            final Block block = event.getPlayer().getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
//            if (!block.getBlockData().getSoundGroup().toString().equals(WOOD_SOUND_GROUP)) {
//                return;
//            }
//            final Location location = block.getLocation();
//            final BukkitScheduler scheduler = Bukkit.getScheduler();
//
//            if (type == EnumWrappers.PlayerDigType.START_DESTROY_BLOCK) {
//                blockWithActiveSound.remove(block);
//
//                if (blockBreakers.containsKey(location)) {
//                    blockBreakers.get(location).cancelTasks(GardensFurniture.plugin);
//                }
//
//                blockBreakers.put(location, scheduler);
//                scheduler.runTaskTimer(GardensFurniture.plugin, () -> {
//                    if (!blockWithActiveSound.contains(block)) {
//                        blockWithActiveSound.add(block);
//
//                        block.getWorld().playSound(block.getLocation(), VANILLA_WOOD_HIT, SoundCategory.BLOCKS, VANILLA_HIT_VOLUME, VANILLA_HIT_PITCH);
//
//                        scheduler.runTaskLater(GardensFurniture.plugin, () -> blockWithActiveSound.remove(block), 3);
//                    }
//                }, 3L, 4L);
//            } else {
//                scheduler.cancelTasks(GardensFurniture.plugin);
//                blockBreakers.remove(location);
//            }
//        }
//    };
//
//    public void registerListener() {
//        GardensFurniture.protocolManager.addPacketListener(digListener);
//    }
//
//}
