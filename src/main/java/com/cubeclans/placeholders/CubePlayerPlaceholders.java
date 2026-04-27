package com.cubeclans.placeholders;

import com.cubeclans.CubeClans;
import com.cubeclans.models.Clan;
import com.cubeclans.utils.ColorUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CubePlayerPlaceholders extends PlaceholderExpansion implements Relational {

    private final CubeClans plugin;

    public CubePlayerPlaceholders(CubeClans plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "cube";
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

        if (params.equalsIgnoreCase("playername")) {
            String color = getSelfColor(player);
            return ColorUtil.translate(color + getPlayerNameSafe(player));
        }

        if (params.toLowerCase().startsWith("playername_")) {
            String targetName = params.substring("playername_".length());
            if (targetName.isEmpty()) {
                return ColorUtil.translate("&e" + getPlayerNameSafe(player));
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            String color = getRelationColor(player, target);
            return ColorUtil.translate(color + getPlayerNameSafe(target));
        }

        return null;
    }

    @Override
    public String onPlaceholderRequest(Player one, Player two, String identifier) {
        if (!identifier.equalsIgnoreCase("playername")) {
            return null;
        }

        String color = getRelationColor(one, two);
        return ColorUtil.translate(color + getPlayerNameSafe(two));
    }

    private String getSelfColor(OfflinePlayer player) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        return clan != null ? getConfiguredColor("member", "&a") : getConfiguredColor("neutral", "&e");
    }

    private String getRelationColor(OfflinePlayer viewer, OfflinePlayer target) {
        if (viewer == null || target == null) {
            return getConfiguredColor("neutral", "&e");
        }

        Clan viewerClan = plugin.getClanManager().getPlayerClan(viewer.getUniqueId());
        Clan targetClan = plugin.getClanManager().getPlayerClan(target.getUniqueId());

        if (viewerClan != null && targetClan != null) {
            if (viewerClan.getName().equalsIgnoreCase(targetClan.getName())) {
                return getConfiguredColor("member", "&a");
            }

            if (viewerClan.isEnemy(targetClan.getName()) || targetClan.isEnemy(viewerClan.getName())) {
                return getConfiguredColor("enemy", "&c");
            }

            if (viewerClan.isAlly(targetClan.getName()) || targetClan.isAlly(viewerClan.getName())) {
                return getConfiguredColor("ally", "&b");
            }
        }

        return getConfiguredColor("neutral", "&e");
    }

    private String getConfiguredColor(String relation, String defaultColor) {
        return plugin.getConfig().getString("placeholders.playername-colors." + relation, defaultColor);
    }

    private String getPlayerNameSafe(OfflinePlayer player) {
        String name = player.getName();
        return name != null ? name : "Unknown";
    }
}