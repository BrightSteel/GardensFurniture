package com.gardensmc.gardensfurniture.util;

import com.gardensmc.gardensfurniture.GardensFurniture;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class ProtectionUtil {

    public static boolean canBuild(Location location, Player player) {
        if (!GardensFurniture.isGriefPreventionHooked) {
            return true;
        }
        return GriefPrevention.instance.allowBuild(player, location) == null;
    }

    @Nullable
    public static ClaimPermission getClaimPermission(Location location, Player player) {
        if (!GriefPrevention.instance.claimsEnabledForWorld(location.getWorld())) {
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

    public static boolean hasClaimPermission(Location location, Player player, ClaimPermission claimPermission) {
        var permission = getClaimPermission(location, player);
        if (permission == null) {
            return true;
        }
        return claimPermission.isGrantedBy(permission);
    }
}
