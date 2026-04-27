package com.cubeclans.placeholders;

import com.cubeclans.CubeClans;
import com.cubeclans.models.Clan;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class ClanPlaceholders extends PlaceholderExpansion {

    private final CubeClans plugin;

    public ClanPlaceholders(CubeClans plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "cubeclans";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        // Configurable fallback values when the player has no clan
        String noClanName = com.cubeclans.utils.ColorUtil.translate(plugin.getConfig().getString("placeholders.no-clan-name", "[none]"));
        String noClanNameColor = com.cubeclans.utils.ColorUtil.translate(plugin.getConfig().getString("placeholders.no-clan-name-color", "&7[none]"));

        // %cubeclans_name% (without color codes)
        if (params.equalsIgnoreCase("name")) {
            return clan != null ? clan.getName() : noClanName;
        }

        // %cubeclans_name_color% (colored name)
        if (params.equalsIgnoreCase("name_color")) {
            return clan != null ? com.cubeclans.utils.ColorUtil.translate(clan.getColorCode() + clan.getName()) : noClanNameColor;
        }

        // %cubeclans_leader%
        if (params.equalsIgnoreCase("leader")) {
            if (clan == null) return "None";
            return Bukkit.getOfflinePlayer(clan.getLeader()).getName();
        }

        // %cubeclans_members%
        if (params.equalsIgnoreCase("members")) {
            return clan != null ? String.valueOf(clan.getMemberCount()) : "0";
        }

        // %cubeclans_max_members%
        if (params.equalsIgnoreCase("max_members")) {
            return String.valueOf(plugin.getConfig().getInt("settings.max-clan-members", 10));
        }

        // %cubeclans_is_leader%
        if (params.equalsIgnoreCase("is_leader")) {
            if (clan == null) return "false";
            return String.valueOf(clan.isLeader(player.getUniqueId()));
        }

        // %cubeclans_has_clan%
        if (params.equalsIgnoreCase("has_clan")) {
            return String.valueOf(clan != null);
        }

        // %cubeclans_created%
        if (params.equalsIgnoreCase("created")) {
            return clan != null ? clan.getFormattedCreationDate() : "N/A";
        }

        // %cubeclans_description%
        if (params.equalsIgnoreCase("description")) {
            return clan != null ? clan.getDescription() : "None";
        }

        // %cubeclans_total_clans%
        if (params.equalsIgnoreCase("total_clans")) {
            return String.valueOf(plugin.getClanManager().getClanCount());
        }

        // %cubeclans_joined%
        if (params.equalsIgnoreCase("joined")) {
            if (clan == null) return "N/A";
            return clan.getFormattedJoinDate(player.getUniqueId());
        }

        // %cubeclans_role%
        if (params.equalsIgnoreCase("role")) {
            if (clan == null) return "None";
            return clan.isLeader(player.getUniqueId()) ? "Leader" : "Member";
        }

        // %cubeclans_allies%
        if (params.equalsIgnoreCase("allies")) {
            return clan != null ? String.valueOf(clan.getAllyCount()) : "0";
        }

        // %cubeclans_max_allies%
        if (params.equalsIgnoreCase("max_allies")) {
            return String.valueOf(plugin.getConfig().getInt("settings.max-clan-allies", 3));
        }

        // %cubeclans_ally_names%
        if (params.equalsIgnoreCase("ally_names")) {
            if (clan == null || clan.getAllies().isEmpty()) return "None";
            return String.join(", ", clan.getAllies());
        }

        // %cubeclans_enemies%
        if (params.equalsIgnoreCase("enemies")) {
            return clan != null ? String.valueOf(clan.getEnemies().size()) : "0";
        }

        // %cubeclans_max_enemies%
        if (params.equalsIgnoreCase("max_enemies")) {
            return String.valueOf(plugin.getConfig().getInt("settings.max-clan-enemies", 5));
        }

        // %cubeclans_enemy_names%
        if (params.equalsIgnoreCase("enemy_names")) {
            if (clan == null || clan.getEnemies().isEmpty()) return "None";
            return String.join(", ", clan.getEnemies());
        }

        // %cubeclans_bank%
        if (params.equalsIgnoreCase("bank")) {
            return clan != null ? String.format("%.2f", clan.getBankBalance()) : "0.00";
        }

        // %cubeclans_kdr%
        if (params.equalsIgnoreCase("kdr")) {
            return clan != null ? String.format("%.2f", clan.getKDR()) : "0.00";
        }

        // %cubeclans_kills%
        if (params.equalsIgnoreCase("kills")) {
            return clan != null ? String.valueOf(clan.getTotalKills()) : "0";
        }

        // %cubeclans_deaths%
        if (params.equalsIgnoreCase("deaths")) {
            return clan != null ? String.valueOf(clan.getTotalDeaths()) : "0";
        }

        // %cubeclans_player_kill_percent% - percentage of clan kills by this player
        if (params.equalsIgnoreCase("player_kill_percent")) {
            if (clan == null) return "0.00";
            int playerKills = clan.getMemberKills(player.getUniqueId());
            int totalKills = clan.getTotalKills();
            if (totalKills <= 0) return "0.00";
            double percent = (playerKills / (double) totalKills) * 100.0;
            return String.format("%.2f", percent);
        }

        // %cubeclans_player_kills% - kills by this player
        if (params.equalsIgnoreCase("player_kills")) {
            if (clan == null) return "0";
            return String.valueOf(clan.getMemberKills(player.getUniqueId()));
        }

        // %cubeclans_player_deaths% - deaths by this player
        if (params.equalsIgnoreCase("player_deaths")) {
            if (clan == null) return "0";
            return String.valueOf(clan.getMemberDeaths(player.getUniqueId()));
        }

        // %cubeclans_player_kdr% - KDR of this player
        if (params.equalsIgnoreCase("player_kdr")) {
            if (clan == null) return "0.00";
            int playerKills = clan.getMemberKills(player.getUniqueId());
            int playerDeaths = clan.getMemberDeaths(player.getUniqueId());
            if (playerDeaths == 0) {
                return String.format("%.2f", (double) playerKills);
            }
            return String.format("%.2f", (double) playerKills / playerDeaths);
        }

        // Aliases for clan_player_* (used in config)
        if (params.equalsIgnoreCase("clan_player_kills")) {
            if (clan == null) return "0";
            return String.valueOf(clan.getMemberKills(player.getUniqueId()));
        }

        if (params.equalsIgnoreCase("clan_player_deaths")) {
            if (clan == null) return "0";
            return String.valueOf(clan.getMemberDeaths(player.getUniqueId()));
        }

        if (params.equalsIgnoreCase("clan_player_kdr")) {
            if (clan == null) return "0.00";
            int playerKills = clan.getMemberKills(player.getUniqueId());
            int playerDeaths = clan.getMemberDeaths(player.getUniqueId());
            if (playerDeaths == 0) {
                return String.format("%.2f", (double) playerKills);
            }
            return String.format("%.2f", (double) playerKills / playerDeaths);
        }

        if (params.equalsIgnoreCase("clan_player_kill_percent")) {
            if (clan == null) return "0.00";
            int playerKills = clan.getMemberKills(player.getUniqueId());
            int totalKills = clan.getTotalKills();
            if (totalKills <= 0) return "0.00";
            double percent = (playerKills / (double) totalKills) * 100.0;
            return String.format("%.2f", percent);
        }

        return null;
    }
}

