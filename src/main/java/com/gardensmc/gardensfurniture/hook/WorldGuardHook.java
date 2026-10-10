package com.gardensmc.gardensfurniture.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * WorldGuard's say on building, for furniture placed by our own code (adding to a block already there, a block
 * replaced by right-clicking it), which WorldGuard never sees as a placement. Only loaded when WorldGuard is.
 */
public final class WorldGuardHook {

    private WorldGuardHook() {
    }

    /** Whether WorldGuard lets the player build at the location (regions, their build flag, its bypass). */
    public static boolean canBuild(Player player, Location location) {
        var localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        var platform = WorldGuard.getInstance().getPlatform();
        if (platform.getSessionManager().hasBypass(localPlayer, BukkitAdapter.adapt(location.getWorld()))) return true;
        return platform.getRegionContainer().createQuery().testBuild(BukkitAdapter.adapt(location), localPlayer);
    }
}
