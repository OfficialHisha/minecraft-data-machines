package com.hishacorp.dataMachines.guards;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class GriefPreventionGuard {

    /**
     * Checks if a player can interact with a machine at the given location based on GriefPrevention claims.
     *
     * @param player   The player attempting to interact.
     * @param location The location of the machine.
     * @return true if the player can interact, false otherwise.
     */
    public static boolean canInteract(Player player, Location location) {
        if (!Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")) {
            return true;
        }

        GriefPrevention gp = GriefPrevention.instance;
        if (gp == null) {
            return true;
        }

        PlayerData playerData = gp.dataStore.getPlayerData(player.getUniqueId());
        if (playerData == null || playerData.ignoreClaims) {
            return true;
        }

        Claim claim = gp.dataStore.getClaimAt(location, false, playerData.lastClaim);
        if (claim == null) {
            return true;
        }

        // Update last claim for performance optimization in GriefPrevention
        playerData.lastClaim = claim;

        // checkPermission returns null if the permission is granted
        return claim.checkPermission(player, ClaimPermission.Inventory, null) == null;
    }
}
