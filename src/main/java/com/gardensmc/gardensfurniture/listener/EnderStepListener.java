package com.gardensmc.gardensfurniture.listener;

import com.gardensmc.gardensfurniture.GardensFurniture;
import net.kyori.adventure.key.Key;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "Ender Step": double-tap sneak to raytrace to the block you're looking at and teleport there.
 * Only meaningful on armor - the range scales with how many equipped armor pieces carry the mechanic
 * ({@link #DISTANCE_PER_ARMOR} blocks each). Gated by a 10s cooldown that renders on the armor items
 * (they share the {@link #COOLDOWN_GROUP} use-cooldown group, set in {@code ItemRegistry}).
 */
public class EnderStepListener implements Listener {

    // shared use-cooldown group: setting/checking it drives both the gate and the on-item overlay.
    public static final Key COOLDOWN_GROUP = Key.key("gardensfurniture", "ender_step");
    public static final float COOLDOWN_SECONDS = 10f;
    private static final int COOLDOWN_TICKS = (int) (COOLDOWN_SECONDS * 20);

    private static final long DOUBLE_TAP_WINDOW_MS = 400;
    // blocks of range each equipped ender_step armor piece contributes (also shown in item lore).
    public static final int DISTANCE_PER_ARMOR = 3;

    // last time each player pressed shift (entered sneak), for double-tap detection.
    private final Map<UUID, Long> lastSneakStart = new HashMap<>();

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) {
            return; // only react to pressing shift, not releasing it
        }
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastSneakStart.get(uuid);
        if (last != null && now - last <= DOUBLE_TAP_WINDOW_MS) {
            lastSneakStart.remove(uuid); // consume so a third tap doesn't immediately retrigger
            tryEnderStep(player);
        } else {
            lastSneakStart.put(uuid, now);
        }
    }

    private void tryEnderStep(Player player) {
        int armorCount = countEnderStepArmor(player);
        if (armorCount == 0) {
            return; // mechanic only does anything with the armor equipped
        }
        if (player.getCooldown(COOLDOWN_GROUP) > 0) {
            return; // still recharging
        }

        double maxDistance = DISTANCE_PER_ARMOR * armorCount;
        RayTraceResult result = player.rayTraceBlocks(maxDistance, FluidCollisionMode.NEVER);
        if (result == null || result.getHitBlock() == null || result.getHitBlockFace() == null) {
            return; // nothing to step to
        }
        Location destination = safeDestination(player, result);
        if (destination == null) {
            return; // no room to stand at the target
        }

        Location origin = player.getLocation();
        playBlinkEffects(origin);
        player.teleport(destination);
        playBlinkEffects(destination);
        player.setCooldown(COOLDOWN_GROUP, COOLDOWN_TICKS); // gate + on-armor cooldown overlay
    }

    // the air block against the surface being looked at, centered and facing the player's direction.
    private Location safeDestination(Player player, RayTraceResult result) {
        Block against = result.getHitBlock().getRelative(result.getHitBlockFace());
        if (!against.isPassable() || !against.getRelative(org.bukkit.block.BlockFace.UP).isPassable()) {
            return null; // need two blocks of head room
        }
        Location destination = against.getLocation().add(0.5, 0, 0.5);
        destination.setYaw(player.getLocation().getYaw());
        destination.setPitch(player.getLocation().getPitch());
        return destination;
    }

    private void playBlinkEffects(Location location) {
        location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        location.getWorld().spawnParticle(Particle.PORTAL, location.clone().add(0, 1, 0), 40, 0.3, 0.6, 0.3, 0.4);
    }

    private int countEnderStepArmor(Player player) {
        int count = 0;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            var customItem = GardensFurniture.itemRegistry.getCustomItem(armor);
            if (customItem != null && customItem.getMechanics() != null && customItem.getMechanics().isEnderStep()) {
                count++;
            }
        }
        return count;
    }
}
