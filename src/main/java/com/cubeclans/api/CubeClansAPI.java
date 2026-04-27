package com.cubeclans.api;

import com.cubeclans.CubeClans;
import com.cubeclans.managers.ClanManager;
import com.cubeclans.models.Clan;
import com.cubeclans.models.ClanPermission;
import com.cubeclans.models.ClanRank;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * CubeClans public API.
 *
 * Usage in another plugin:
 *
 *   // plugin.yml must declare:  depend: [CubeClans]
 *
 *   Clan clan = CubeClansAPI.getPlayerClan(player.getUniqueId());
 *   if (clan != null) {
 *       player.sendMessage("Your clan: " + clan.getName());
 *   }
 */
public final class CubeClansAPI {

    private static CubeClans instance;

    private CubeClansAPI() {}

    // Called internally by CubeClans on enable
    public static void init(CubeClans plugin) {
        instance = plugin;
    }

    private static ClanManager manager() {
        if (instance == null) {
            throw new IllegalStateException("CubeClans is not loaded or has not been enabled yet.");
        }
        return instance.getClanManager();
    }

    // -------------------------------------------------------------------------
    // Player queries
    // -------------------------------------------------------------------------

    /** Returns the clan the player belongs to, or null if not in one. */
    public static Clan getPlayerClan(UUID playerUUID) {
        return manager().getPlayerClan(playerUUID);
    }

    /** Returns true if the player is in any clan. */
    public static boolean isInClan(UUID playerUUID) {
        return manager().isInClan(playerUUID);
    }

    /** Returns the rank name of a player inside their clan, or null if not in one. */
    public static String getPlayerRankName(UUID playerUUID) {
        Clan clan = manager().getPlayerClan(playerUUID);
        if (clan == null) return null;
        return clan.getMemberRank(playerUUID);
    }

    /** Returns the ClanRank object for the given rank name, or null if not found. */
    public static ClanRank getRank(String rankName) {
        return instance.getRank(rankName);
    }

    /**
     * Returns true if the player has the given permission in their clan.
     * Leaders always return true. Returns false if the player is not in a clan.
     */
    public static boolean hasPermission(UUID playerUUID, ClanPermission permission) {
        Clan clan = manager().getPlayerClan(playerUUID);
        if (clan == null) return false;
        return clan.hasPermission(playerUUID, permission, instance);
    }

    // -------------------------------------------------------------------------
    // Clan queries
    // -------------------------------------------------------------------------

    /** Returns the clan with the given name, or null if it does not exist. */
    public static Clan getClan(String name) {
        return manager().getClan(name);
    }

    /** Returns true if a clan with the given name exists. */
    public static boolean clanExists(String name) {
        return manager().clanExists(name);
    }

    /** Returns all clans currently loaded. */
    public static Collection<Clan> getAllClans() {
        return manager().getAllClans();
    }

    /** Returns a list of all clan names. */
    public static List<String> getAllClanNames() {
        return manager().getAllClanNames();
    }

    /** Returns the total number of clans. */
    public static int getClanCount() {
        return manager().getClanCount();
    }

    // -------------------------------------------------------------------------
    // Clan relationships
    // -------------------------------------------------------------------------

    /** Returns true if clan1 and clan2 are allies. */
    public static boolean areAllies(String clan1, String clan2) {
        Clan c = manager().getClan(clan1);
        return c != null && c.isAlly(clan2);
    }

    /** Returns true if clan1 and clan2 are enemies. */
    public static boolean areEnemies(String clan1, String clan2) {
        Clan c = manager().getClan(clan1);
        return c != null && c.isEnemy(clan2);
    }

    /**
     * Returns true if two players are in the same clan.
     * Returns false if either player is not in a clan.
     */
    public static boolean areInSameClan(UUID player1, UUID player2) {
        Clan c1 = manager().getPlayerClan(player1);
        if (c1 == null) return false;
        return c1.isMember(player2);
    }

    /**
     * Returns true if two players are in allied clans.
     * Returns false if either is not in a clan or clans are not allied.
     */
    public static boolean areAllied(UUID player1, UUID player2) {
        Clan c1 = manager().getPlayerClan(player1);
        Clan c2 = manager().getPlayerClan(player2);
        if (c1 == null || c2 == null) return false;
        return c1.isAlly(c2.getName());
    }

    // -------------------------------------------------------------------------
    // Advanced access
    // -------------------------------------------------------------------------

    /**
     * Returns the raw ClanManager for advanced operations not covered by this API.
     * Use with care — changes made through the manager are authoritative.
     */
    public static ClanManager getClanManager() {
        return manager();
    }
}
