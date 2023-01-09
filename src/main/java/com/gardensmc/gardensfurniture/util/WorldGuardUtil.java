package com.gardensmc.gardensfurniture.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.entity.Player;

public class WorldGuardUtil {

    public static RegionManager getWorldRegions(org.bukkit.World bukkit_world) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        World world = BukkitAdapter.adapt(bukkit_world);
        return container.get(world);
    }

    public static boolean canBuildInRegion(Player player) {
        if (player.hasPermission("gardens.bypass_regions")) return true;
        ProtectedRegion rg = WorldGuardUtil.getWorldRegions(player.getWorld()).getRegion("__global__");
        if (rg == null) {
            GardensFurniture.plugin.getLogger().info("No worldguard region exists, cannot verify build permissions");
            return false;
        }
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

        return rg.isOwner(localPlayer) || rg.isMember(localPlayer);
    }
}
