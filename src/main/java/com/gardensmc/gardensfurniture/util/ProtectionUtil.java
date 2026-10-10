package com.gardensmc.gardensfurniture.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Objects;

public class ProtectionUtil {

    /**
     * Whether the player may build at the location: GriefPrevention's claims and WorldGuard's regions both have to
     * allow it (either one missing counts as allowing). Our own placements (adding to a clover, swapping a block)
     * never go through vanilla, so neither plugin would stop them otherwise.
     */
    public static boolean canBuild(Location location, Player player) {
        if (player != null && worldGuardHooked() && !com.gardensmc.gardensfurniture.hook.WorldGuardHook.canBuild(player, location)) {
            return false;
        }
        if (!GardensFurniture.isGriefPreventionHooked) {
            return true;
        }
        return GriefPrevention.instance.allowBuild(player, location) == null;
    }

    // checked before WorldGuardHook is touched, so its classes never load without WorldGuard
    private static Boolean worldGuard;

    private static boolean worldGuardHooked() {
        if (worldGuard == null) worldGuard = org.bukkit.Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
        return worldGuard;
    }

    @Nullable
    public static ClaimPermission getClaimPermission(Location location, Player player) {
        if (!GardensFurniture.isGriefPreventionHooked || !GriefPrevention.instance.claimsEnabledForWorld(location.getWorld())) {
            return null;
        }
        var dataStore = GriefPrevention.instance.dataStore;
        PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());
        Claim claim = dataStore.getClaimAt(location, false, playerData.lastClaim);
        if (claim == null) {
            return null;
        }
        playerData.lastClaim = claim; // caches claim, ig for future checks
        return claim.getPermission(player.getUniqueId().toString());
    }

    // access trust: using things (sitting, switching lights) without changing them. The GriefPrevention
    // class is only touched once we know it's installed.
    public static boolean canUse(Location location, Player player) {
        return !GardensFurniture.isGriefPreventionHooked || hasClaimPermission(location, player, ClaimPermission.Access);
    }

    public static boolean hasClaimPermission(Location location, Player player, ClaimPermission claimPermission) {
        var permission = getClaimPermission(location, player);
        if (permission == null) {
            return true;
        }
        return claimPermission.isGrantedBy(permission);
    }

    /**
     * Mirrors GriefPrevention's own tree rule: something growing from {@code origin} may only change
     * {@code cell} if the cell is unclaimed or claimed by the same owner as the origin.
     */
    public static boolean canGrowInto(Location origin, Location cell) {
        if (!GardensFurniture.isGriefPreventionHooked) {
            return true;
        }
        var dataStore = GriefPrevention.instance.dataStore;
        Claim cellClaim = dataStore.getClaimAt(cell, true, null);
        if (cellClaim == null) {
            return true;
        }
        Claim originClaim = dataStore.getClaimAt(origin, true, null);
        return originClaim != null && Objects.equals(originClaim.getOwnerID(), cellClaim.getOwnerID());
    }

    /**
     * True if a claim owned by someone other than the origin's claim owner lies within {@code radius}
     * blocks (x/z). Used where growth can't be checked block by block (datapack features).
     */
    public static boolean hasForeignClaimNear(Location origin, int radius) {
        if (!GardensFurniture.isGriefPreventionHooked) {
            return false;
        }
        var dataStore = GriefPrevention.instance.dataStore;
        Claim originClaim = dataStore.getClaimAt(origin, true, null);
        var originOwner = originClaim == null ? null : originClaim.getOwnerID();
        int minX = origin.getBlockX() - radius, maxX = origin.getBlockX() + radius;
        int minZ = origin.getBlockZ() - radius, maxZ = origin.getBlockZ() + radius;
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                for (Claim claim : dataStore.getClaims(cx, cz)) {
                    if (!Objects.equals(claim.getLesserBoundaryCorner().getWorld(), origin.getWorld())
                            || (originOwner != null && originOwner.equals(claim.getOwnerID()))) {
                        continue;
                    }
                    var lesser = claim.getLesserBoundaryCorner();
                    var greater = claim.getGreaterBoundaryCorner();
                    if (lesser.getBlockX() <= maxX && greater.getBlockX() >= minX
                            && lesser.getBlockZ() <= maxZ && greater.getBlockZ() >= minZ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
