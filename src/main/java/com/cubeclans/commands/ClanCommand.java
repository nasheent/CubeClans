package com.cubeclans.commands;

import com.cubeclans.CubeClans;
import com.cubeclans.managers.ClanManager;
import com.cubeclans.managers.ClanManager.ClanCreationResult;
import com.cubeclans.menus.ClanMenu;
import com.cubeclans.models.Clan;
import com.cubeclans.models.ClanPermission;
import com.cubeclans.models.ClanRank;
import com.cubeclans.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class ClanCommand implements CommandExecutor, TabCompleter {

    private final CubeClans plugin;
    private final ClanManager clanManager;
    private final Map<UUID, Long> disbandConfirmation;

    public ClanCommand(CubeClans plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.disbandConfirmation = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        // Open menu if no arguments
        if (args.length == 0) {
            if (!player.hasPermission("cubeclans.use")) {
                player.sendMessage(getMessage("no-permission"));
                return true;
            }
            new ClanMenu(plugin, player).openMainMenu();
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                handleCreate(player, args);
                break;
            case "delete":
            case "disband":
                handleDisband(player);
                break;
            case "invite":
                handleInvite(player, args);
                break;
            case "accept":
            case "join":
                handleAccept(player, args);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "kick":
                handleKick(player, args);
                break;
            case "info":
                handleInfo(player, args);
                break;
            case "members":
                handleMembers(player);
                break;
            case "admin":
                handleAdmin(player, args);
                break;
            case "ally":
                handleAlly(player, args);
                break;
            case "enemy":
                handleEnemy(player, args);
                break;
            case "pvp":
                handlePvp(player, args);
                break;
            case "chat":
                handleChatToggle(player, args);
                break;
            case "help":
                handleHelp(player, args);
                break;
            case "bank":
                handleBank(player, args);
                break;
            case "list":
                handleList(player);
                break;
            case "promote":
                handlePromote(player, args);
                break;
            case "demote":
                handleDemote(player, args);
                break;
            default:
                new ClanMenu(plugin, player).openMainMenu();
                break;
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (!player.hasPermission("cubeclans.create")) {
            player.sendMessage(getMessage("no-permission"));
            return;
        }

        if (clanManager.isInClan(player.getUniqueId())) {
            player.sendMessage(getMessage("already-in-clan"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(getMessage("clan-create-usage"));
            return;
        }

        String clanName = args[1];
        int minLength = plugin.getConfig().getInt("settings.min-clan-name-length", 3);
        int maxLength = plugin.getConfig().getInt("settings.max-clan-name-length", 16);

        if (clanName.length() < minLength) {
            player.sendMessage(getMessage("clan-name-too-short")
                    .replace("%min%", String.valueOf(minLength)));
            return;
        }

        if (clanName.length() > maxLength) {
            player.sendMessage(getMessage("clan-name-too-long")
                    .replace("%max%", String.valueOf(maxLength)));
            return;
        }

        if (clanManager.clanExists(clanName)) {
            player.sendMessage(getMessage("clan-already-exists"));
            return;
        }

        // Strip color codes from clan name
        String cleanName = com.cubeclans.utils.ColorUtil.strip(clanName);
        ClanCreationResult result = clanManager.createClanWithResult(cleanName, player.getUniqueId());

        if (result == ClanCreationResult.SUCCESS) {
            Clan clan = clanManager.getClan(cleanName);
            if (clan != null) {
                player.sendMessage(getMessage("clan-created")
                        .replace("%clan%", clan.getName()));
                clanManager.saveClans();
            }
        } else if (result == ClanCreationResult.INSUFFICIENT_FUNDS) {
            double clanCost = plugin.getConfig().getDouble("settings.clan-creation-cost", 300);
            player.sendMessage(getMessage("insufficient-funds")
                    .replace("%cost%", String.valueOf((int) clanCost)));
        } else if (result == ClanCreationResult.CLAN_EXISTS) {
            player.sendMessage(getMessage("clan-already-exists"));
        }
    }

    private void handleDisband(Player player) {
        if (!player.hasPermission("cubeclans.disband")) {
            player.sendMessage(getMessage("no-permission"));
            return;
        }

        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(getMessage("not-in-clan"));
            return;
        }

        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage(getMessage("clan-not-leader"));
            return;
        }

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check if player has pending confirmation
        if (disbandConfirmation.containsKey(playerId)) {
            long confirmTime = disbandConfirmation.get(playerId);
            long timePassed = (currentTime - confirmTime) / 1000; // seconds
            
            // If less than 10 seconds, proceed with deletion
            if (timePassed <= 10) {
                disbandConfirmation.remove(playerId);
                
                String clanName = clan.getName();
                clanManager.deleteClan(clanName);
                player.sendMessage(getMessage("clan-deleted"));
                clanManager.saveClans();

                // Notify members
                for (UUID memberId : clan.getMembers()) {
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null && !member.getUniqueId().equals(playerId)) {
                        member.sendMessage(getMessage("clan-disbanded-notification"));
                    }
                }
                return;
            } else {
                // Timeout, remove old confirmation
                disbandConfirmation.remove(playerId);
            }
        }
        
        // First time or timeout, request confirmation
        disbandConfirmation.put(playerId, currentTime);
        player.sendMessage(getMessage("disband-confirmation-required")
            .replace("%clan%", clan.getName()));
        
        // Remove confirmation after 10 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (disbandConfirmation.containsKey(playerId)) {
                long storedTime = disbandConfirmation.get(playerId);
                if (storedTime == currentTime) {
                    disbandConfirmation.remove(playerId);
                }
            }
        }, 200L); // 10 seconds = 200 ticks
    }

    private void handleInvite(Player player, String[] args) {
        if (!player.hasPermission("cubeclans.invite")) {
            player.sendMessage(getMessage("no-permission"));
            return;
        }

        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(getMessage("not-in-clan"));
            return;
        }

        // Check clan permission
        if (!clan.hasPermission(player.getUniqueId(), com.cubeclans.models.ClanPermission.INVITE, plugin)) {
            player.sendMessage(getMessage("clan-not-leader"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ColorUtil.translate("&cUsage: /clan invite <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(getMessage("player-not-found"));
            return;
        }

        if (clanManager.isInClan(target.getUniqueId())) {
            player.sendMessage(getMessage("already-in-clan-other"));
            return;
        }

        int maxMembers = plugin.getConfig().getInt("settings.max-clan-members", 10);
        if (clan.getMemberCount() >= maxMembers) {
            player.sendMessage(getMessage("clan-full")
                    .replace("%max%", String.valueOf(maxMembers)));
            return;
        }

        clanManager.invitePlayer(target.getUniqueId(), clan.getName());
        player.sendMessage(getMessage("invite-sent")
                .replace("%player%", target.getName()));
        target.sendMessage(getMessage("invite-received")
                .replace("%clan%", clan.getName()));
    }

    private void handleAccept(Player player, String[] args) {
        if (!player.hasPermission("cubeclans.accept")) {
            player.sendMessage(getMessage("no-permission"));
            return;
        }

        if (clanManager.isInClan(player.getUniqueId())) {
            player.sendMessage(getMessage("already-in-clan"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ColorUtil.translate("&cUsage: /clan accept <clan>"));
            return;
        }

        String clanName = args[1];
        Clan clan = clanManager.getClan(clanName);

        if (clan == null) {
            player.sendMessage(getMessage("clan-not-found"));
            return;
        }

        if (!clanManager.hasInvitation(player.getUniqueId(), clanName)) {
            player.sendMessage(getMessage("no-invitation"));
            return;
        }

        clanManager.addMemberToClan(clan, player.getUniqueId());
        clanManager.removeInvitation(player.getUniqueId(), clanName);
        
        player.sendMessage(getMessage("clan-joined")
                .replace("%clan%", clan.getName()));
        
        // Notify clan members
        for (UUID memberUUID : clan.getMembers()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && !member.equals(player)) {
                member.sendMessage(getMessage("member-joined")
                        .replace("%player%", player.getName()));
            }
        }
        
        clanManager.saveClans();
    }

    private void handleLeave(Player player) {
        if (!player.hasPermission("cubeclans.leave")) {
            player.sendMessage(getMessage("no-permission"));
            return;
        }

        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(getMessage("not-in-clan"));
            return;
        }

        if (clan.isLeader(player.getUniqueId())) {
            player.sendMessage(getMessage("leader-cannot-leave"));
            return;
        }

        clanManager.removeMemberFromClan(clan, player.getUniqueId());
        player.sendMessage(getMessage("clan-left"));

        // Notify remaining members
        for (UUID memberUUID : clan.getMembers()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null) {
                member.sendMessage(getMessage("member-left")
                        .replace("%player%", player.getName()));
            }
        }
        
        clanManager.saveClans();
    }

    private void handleKick(Player player, String[] args) {
        if (!player.hasPermission("cubeclans.kick")) {
            player.sendMessage(getMessage("no-permission"));
            return;
        }

        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(getMessage("not-in-clan"));
            return;
        }

        // Check clan permission
        if (!clan.hasPermission(player.getUniqueId(), com.cubeclans.models.ClanPermission.KICK, plugin)) {
            player.sendMessage(getMessage("clan-not-leader"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ColorUtil.translate("&cUsage: /clan kick <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUUID;
        if (target != null) {
            targetUUID = target.getUniqueId();
        } else {
            // Buscar miembro offline por nombre dentro del clan para evitar UUID inesperados
            UUID found = null;
            String searchName = args[1].toLowerCase();
            for (UUID memberUUID : clan.getMembers()) {
                String offlineName = Bukkit.getOfflinePlayer(memberUUID).getName();
                if (offlineName != null && offlineName.toLowerCase().equals(searchName)) {
                    found = memberUUID;
                    break;
                }
            }
            if (found == null) {
                player.sendMessage(getMessage("player-not-found"));
                return;
            }
            targetUUID = found;
        }

        if (targetUUID.equals(player.getUniqueId())) {
            player.sendMessage(getMessage("cannot-kick-yourself"));
            return;
        }

        if (!clan.isMember(targetUUID)) {
            player.sendMessage(getMessage("not-in-your-clan"));
            return;
        }

        clanManager.removeMemberFromClan(clan, targetUUID);
        player.sendMessage(getMessage("player-kicked")
                .replace("%player%", args[1]));

        if (target != null) {
            target.sendMessage(getMessage("you-were-kicked")
                    .replace("%clan%", clan.getName()));
        }

        // Notify remaining members
        for (UUID memberUUID : clan.getMembers()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && !member.equals(player)) {
                member.sendMessage(getMessage("kicked-broadcast")
                        .replace("%player%", args[1]));
            }
        }
        
        clanManager.saveClans();
    }

    private void handlePromote(Player player, String[] args) {
        if (!player.hasPermission("cubeclans.promote")) {
            player.sendMessage(getMessage("no-permission"));
            return;
        }

        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(getMessage("not-in-clan"));
            return;
        }

        // Check clan permission
        if (!clan.hasPermission(player.getUniqueId(), com.cubeclans.models.ClanPermission.PROMOTE, plugin)) {
            player.sendMessage(getMessage("no-promote-permission"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ColorUtil.translate("&cUsage: /clan promote <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUUID;
        String targetName;
        
        if (target != null) {
            targetUUID = target.getUniqueId();
            targetName = target.getName();
        } else {
            // Search for offline member
            UUID found = null;
            String searchName = args[1].toLowerCase();
            for (UUID memberUUID : clan.getMembers()) {
                String offlineName = Bukkit.getOfflinePlayer(memberUUID).getName();
                if (offlineName != null && offlineName.toLowerCase().equals(searchName)) {
                    found = memberUUID;
                    targetName = offlineName;
                    break;
                }
            }
            if (found == null) {
                player.sendMessage(getMessage("player-not-found"));
                return;
            }
            targetUUID = found;
            targetName = Bukkit.getOfflinePlayer(found).getName();
        }

        if (!clan.isMember(targetUUID)) {
            player.sendMessage(getMessage("not-in-your-clan"));
            return;
        }

        if (clan.isLeader(targetUUID)) {
            player.sendMessage(getMessage("cannot-promote-leader"));
            return;
        }

        // Get current rank
        String currentRankName = clan.getMemberRank(targetUUID);
        ClanRank currentRank = plugin.getRank(currentRankName);
        
        if (currentRank == null) {
            player.sendMessage(getMessage("rank-error"));
            return;
        }

        // Find next rank (higher ladder position)
        ClanRank nextRank = null;
        int targetPosition = currentRank.getLadderPosition() + 1;
        
        for (ClanRank rank : plugin.getRanks().values()) {
            if (rank.getLadderPosition() == targetPosition) {
                nextRank = rank;
                break;
            }
        }

        if (nextRank == null) {
            player.sendMessage(getMessage("already-max-rank"));
            return;
        }

        // Check if player can promote to this rank (can't promote to same or higher rank than self, unless leader)
        if (!clan.isLeader(player.getUniqueId())) {
            String playerRankName = clan.getMemberRank(player.getUniqueId());
            ClanRank playerRank = plugin.getRank(playerRankName);
            
            if (playerRank != null && nextRank.getLadderPosition() >= playerRank.getLadderPosition()) {
                player.sendMessage(getMessage("cannot-promote-higher"));
                return;
            }
        }

        // Promote
        clan.setMemberRank(targetUUID, nextRank.getName());
        clanManager.saveClans();
        
        player.sendMessage(getMessage("player-promoted")
                .replace("%player%", targetName)
                .replace("%rank%", nextRank.getDisplayName()));

        if (target != null) {
            target.sendMessage(getMessage("you-were-promoted")
                    .replace("%rank%", nextRank.getDisplayName()));
        }
    }

    private void handleDemote(Player player, String[] args) {
        if (!player.hasPermission("cubeclans.demote")) {
            player.sendMessage(getMessage("no-permission"));
            return;
        }

        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(getMessage("not-in-clan"));
            return;
        }

        // Check clan permission
        if (!clan.hasPermission(player.getUniqueId(), com.cubeclans.models.ClanPermission.DEMOTE, plugin)) {
            player.sendMessage(getMessage("no-demote-permission"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ColorUtil.translate("&cUsage: /clan demote <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUUID;
        String targetName;
        
        if (target != null) {
            targetUUID = target.getUniqueId();
            targetName = target.getName();
        } else {
            // Search for offline member
            UUID found = null;
            String searchName = args[1].toLowerCase();
            for (UUID memberUUID : clan.getMembers()) {
                String offlineName = Bukkit.getOfflinePlayer(memberUUID).getName();
                if (offlineName != null && offlineName.toLowerCase().equals(searchName)) {
                    found = memberUUID;
                    targetName = offlineName;
                    break;
                }
            }
            if (found == null) {
                player.sendMessage(getMessage("player-not-found"));
                return;
            }
            targetUUID = found;
            targetName = Bukkit.getOfflinePlayer(found).getName();
        }

        if (!clan.isMember(targetUUID)) {
            player.sendMessage(getMessage("not-in-your-clan"));
            return;
        }

        if (clan.isLeader(targetUUID)) {
            player.sendMessage(getMessage("cannot-demote-leader"));
            return;
        }

        // Get current rank
        String currentRankName = clan.getMemberRank(targetUUID);
        ClanRank currentRank = plugin.getRank(currentRankName);
        
        if (currentRank == null) {
            player.sendMessage(getMessage("rank-error"));
            return;
        }

        // Find previous rank (lower ladder position)
        ClanRank prevRank = null;
        int targetPosition = currentRank.getLadderPosition() - 1;
        
        for (ClanRank rank : plugin.getRanks().values()) {
            if (rank.getLadderPosition() == targetPosition) {
                prevRank = rank;
                break;
            }
        }

        if (prevRank == null) {
            player.sendMessage(getMessage("already-min-rank"));
            return;
        }

        // Check if player can demote from this rank (can't demote someone at same or higher rank than self, unless leader)
        if (!clan.isLeader(player.getUniqueId())) {
            String playerRankName = clan.getMemberRank(player.getUniqueId());
            ClanRank playerRank = plugin.getRank(playerRankName);
            
            if (playerRank != null && currentRank.getLadderPosition() >= playerRank.getLadderPosition()) {
                player.sendMessage(getMessage("cannot-demote-higher"));
                return;
            }
        }

        // Demote
        clan.setMemberRank(targetUUID, prevRank.getName());
        clanManager.saveClans();
        
        player.sendMessage(getMessage("player-demoted")
                .replace("%player%", targetName)
                .replace("%rank%", prevRank.getDisplayName()));

        if (target != null) {
            target.sendMessage(getMessage("you-were-demoted")
                    .replace("%rank%", prevRank.getDisplayName()));
        }
    }

    private void handleInfo(Player player, String[] args) {
        Clan clan;
        
        if (args.length >= 2) {
            clan = clanManager.getClan(args[1]);
        } else {
            clan = clanManager.getPlayerClan(player.getUniqueId());
        }

        if (clan == null) {
            player.sendMessage(getMessage("clan-not-found"));
            return;
        }

        // Show updated clan info in chat (without description)
        int maxMembers = plugin.getConfig().getInt("settings.max-clan-members", 10);
        int maxAllies = plugin.getConfig().getInt("settings.max-clan-allies", 3);
        int maxEnemies = plugin.getConfig().getInt("settings.max-clan-enemies", 5);
        int totalKills = clan.getTotalKills();
        int totalDeaths = clan.getTotalDeaths();
        double kdr = clan.getKDR();

        player.sendMessage(ColorUtil.translate("&8&m------------------------"));
        player.sendMessage(ColorUtil.translate("&bClan: " + clan.getColoredName()));
        player.sendMessage(ColorUtil.translate("&7Leader: &f" + Bukkit.getOfflinePlayer(clan.getLeader()).getName()));
        player.sendMessage(ColorUtil.translate("&7Members: &f" + clan.getMemberCount() + "/" + maxMembers));
        player.sendMessage(ColorUtil.translate("&7Created: &f" + clan.getFormattedCreationDate()));
        player.sendMessage(ColorUtil.translate("&7Allies: &a" + clan.getAllyCount() + "&7/&c" + maxAllies));
        player.sendMessage(ColorUtil.translate("&7Enemies: &c" + clan.getEnemies().size() + "&7/&8" + maxEnemies));
        player.sendMessage(ColorUtil.translate("&7Kills: &a" + totalKills + " &7Deaths: &c" + totalDeaths));
        player.sendMessage(ColorUtil.translate("&7KDR: &e" + String.format("%.2f", kdr)));
        player.sendMessage(ColorUtil.translate("&8&m------------------------"));
    }

    private void handleMembers(Player player) {
        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(getMessage("not-in-clan"));
            return;
        }

        new ClanMenu(plugin, player).openMembersMenu(clan, 0);
    }

    private void handleAlly(Player player, String[] args) {
        if (!player.hasPermission("cubeclans.ally")) {
            player.sendMessage(getMessage("no-permission"));
            return;
        }

        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        
        if (clan == null) {
            player.sendMessage(getMessage("not-in-clan"));
            return;
        }

        // Check clan permission
        if (!clan.hasPermission(player.getUniqueId(), com.cubeclans.models.ClanPermission.ALLY, plugin)) {
            player.sendMessage(getMessage("must-be-leader-ally"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ColorUtil.translate("&cUsage: /clan ally <clan|accept|deny|remove|list> [clan]"));
            return;
        }

        String subCommand = args[1].toLowerCase();

        // /clan ally list
        if (subCommand.equals("list")) {
            player.sendMessage(ColorUtil.translate(getMessage("ally-list-header")));
            if (clan.getAllies().isEmpty()) {
                player.sendMessage(ColorUtil.translate(getMessage("ally-list-empty")));
            } else {
                for (String ally : clan.getAllies()) {
                    player.sendMessage(ColorUtil.translate(getMessage("ally-list-item").replace("%clan%", ally)));
                }
            }
            return;
        }

        // /clan ally accept <clan>
        if (subCommand.equals("accept")) {
            if (args.length < 3) {
                player.sendMessage(ColorUtil.translate("&cUsage: /clan ally accept <clan>"));
                return;
            }

            String requestingClan = args[2];
            Clan requester = clanManager.getClan(requestingClan);

            if (requester == null) {
                player.sendMessage(getMessage("clan-not-found"));
                return;
            }

            if (!clan.hasAllyRequest(requester.getName())) {
                player.sendMessage(getMessage("no-ally-request-from-clan"));
                return;
            }

            int maxAllies = plugin.getConfig().getInt("settings.max-clan-allies", 3);
            if (clan.getAllyCount() >= maxAllies) {
                player.sendMessage(getMessage("ally-limit-reached"));
                return;
            }

            if (requester.getAllyCount() >= maxAllies) {
                player.sendMessage(getMessage("ally-target-limit-reached"));
                return;
            }

            clanManager.acceptAllyRequest(clan.getName(), requester.getName());
            player.sendMessage(getMessage("ally-request-accepted").replace("%clan%", requester.getName()));

            // Notify the requester's leader
            Player requesterLeader = Bukkit.getPlayer(requester.getLeader());
            if (requesterLeader != null && requesterLeader.isOnline()) {
                requesterLeader.sendMessage(getMessage("ally-request-accepted").replace("%clan%", clan.getName()));
            }

            clanManager.saveClans();
            return;
        }

        // /clan ally deny <clan>
        if (subCommand.equals("deny")) {
            if (args.length < 3) {
                player.sendMessage(ColorUtil.translate("&cUsage: /clan ally deny <clan>"));
                return;
            }

            String requestingClan = args[2];
            Clan requester = clanManager.getClan(requestingClan);

            if (requester == null) {
                player.sendMessage(getMessage("clan-not-found"));
                return;
            }

            if (!clan.hasAllyRequest(requester.getName())) {
                player.sendMessage(getMessage("no-ally-request-from-clan"));
                return;
            }

            clanManager.denyAllyRequest(clan.getName(), requester.getName());
            player.sendMessage(getMessage("ally-request-denied").replace("%clan%", requester.getName()));
            clanManager.saveClans();
            return;
        }

        // /clan ally remove <clan>
        if (subCommand.equals("remove")) {
            if (args.length < 3) {
                player.sendMessage(ColorUtil.translate("&cUsage: /clan ally remove <clan>"));
                return;
            }

            String allyClan = args[2];
            Clan ally = clanManager.getClan(allyClan);

            if (ally == null) {
                player.sendMessage(getMessage("clan-not-found"));
                return;
            }

            if (!clan.isAlly(ally.getName())) {
                player.sendMessage(ColorUtil.translate("&cYou are not allies with that clan!"));
                return;
            }

            clanManager.removeAlliance(clan.getName(), ally.getName());
            player.sendMessage(getMessage("ally-removed").replace("%clan%", ally.getName()));

            // Notify the ally's leader
            Player allyLeader = Bukkit.getPlayer(ally.getLeader());
            if (allyLeader != null && allyLeader.isOnline()) {
                allyLeader.sendMessage(getMessage("ally-removed").replace("%clan%", clan.getName()));
            }

            clanManager.saveClans();
            return;
        }

        // /clan ally <clan> - Send ally request
        String targetClan = args[1];
        Clan target = clanManager.getClan(targetClan);

        if (target == null) {
            player.sendMessage(getMessage("clan-not-found"));
            return;
        }

        if (target.getName().equalsIgnoreCase(clan.getName())) {
            player.sendMessage(getMessage("cannot-ally-yourself"));
            return;
        }

        if (clan.isAlly(target.getName())) {
            player.sendMessage(getMessage("ally-already-exists"));
            return;
        }

        int maxAllies = plugin.getConfig().getInt("settings.max-clan-allies", 3);
        if (clan.getAllyCount() >= maxAllies) {
            player.sendMessage(getMessage("ally-limit-reached"));
            return;
        }

        if (target.getAllyCount() >= maxAllies) {
            player.sendMessage(getMessage("ally-target-limit-reached"));
            return;
        }

        clanManager.sendAllyRequest(clan.getName(), target.getName());
        player.sendMessage(getMessage("ally-request-sent").replace("%clan%", target.getName()));

        // Notify target clan leader
        Player targetLeader = Bukkit.getPlayer(target.getLeader());
        if (targetLeader != null && targetLeader.isOnline()) {
            targetLeader.sendMessage(getMessage("ally-request-received").replace("%clan%", clan.getName()));
        }

        clanManager.saveClans();
    }

    private void handleEnemy(Player player, String[] args) {
        if (!player.hasPermission("cubeclans.enemy")) {
            player.sendMessage(getMessage("no-permission"));
            return;
        }

        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        
        if (clan == null) {
            player.sendMessage(getMessage("not-in-clan"));
            return;
        }

        // Check clan permission
        if (!clan.hasPermission(player.getUniqueId(), com.cubeclans.models.ClanPermission.ENEMY, plugin)) {
            player.sendMessage(getMessage("must-be-leader-ally"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ColorUtil.translate("&cUsage: /clan enemy <clan|remove|list> [clan]"));
            return;
        }

        String subCommand = args[1].toLowerCase();

        // /clan enemy list
        if (subCommand.equals("list")) {
            player.sendMessage(ColorUtil.translate(getMessage("enemy-list-header")));
            if (clan.getEnemies().isEmpty()) {
                player.sendMessage(ColorUtil.translate(getMessage("enemy-list-empty")));
            } else {
                for (String enemy : clan.getEnemies()) {
                    player.sendMessage(ColorUtil.translate(getMessage("enemy-list-item").replace("%clan%", enemy)));
                }
            }
            return;
        }

        // /clan enemy remove <clan>
        if (subCommand.equals("remove")) {
            if (args.length < 3) {
                player.sendMessage(ColorUtil.translate("&cUsage: /clan enemy remove <clan>"));
                return;
            }

            String enemyClanName = args[2];
            Clan enemyClan = clanManager.getClan(enemyClanName);

            if (enemyClan == null) {
                player.sendMessage(getMessage("clan-not-found"));
                return;
            }

            if (!clan.isEnemy(enemyClan.getName())) {
                player.sendMessage(ColorUtil.translate(getMessage("not-enemy-with-clan")));
                return;
            }

            clan.removeEnemy(enemyClan.getName());
            enemyClan.removeEnemy(clan.getName());
            player.sendMessage(getMessage("enemy-removed").replace("%clan%", enemyClan.getName()));

            // Notify the enemy's leader
            Player enemyLeader = Bukkit.getPlayer(enemyClan.getLeader());
            if (enemyLeader != null && enemyLeader.isOnline()) {
                enemyLeader.sendMessage(getMessage("enemy-removed-by").replace("%clan%", clan.getName()));
            }

            clanManager.saveClans();
            return;
        }

        // /clan enemy <clan> - Toggle enemy status
        String targetClanName = args[1];
        Clan targetClan = clanManager.getClan(targetClanName);

        if (targetClan == null) {
            player.sendMessage(getMessage("clan-not-found"));
            return;
        }

        if (targetClan.getName().equalsIgnoreCase(clan.getName())) {
            player.sendMessage(getMessage("cannot-enemy-yourself"));
            return;
        }

        // If already enemy, remove
        if (clan.isEnemy(targetClan.getName())) {
            clan.removeEnemy(targetClan.getName());
            targetClan.removeEnemy(clan.getName());
            player.sendMessage(getMessage("enemy-removed").replace("%clan%", targetClan.getName()));

            // Notify target clan leader
            Player targetLeader = Bukkit.getPlayer(targetClan.getLeader());
            if (targetLeader != null && targetLeader.isOnline()) {
                targetLeader.sendMessage(getMessage("enemy-removed-by").replace("%clan%", clan.getName()));
            }
        } else {
            // Add as enemy
            int maxEnemies = plugin.getConfig().getInt("settings.max-clan-enemies", 5);
            if (clan.getEnemies().size() >= maxEnemies) {
                player.sendMessage(getMessage("enemy-limit-reached"));
                return;
            }

            if (targetClan.getEnemies().size() >= maxEnemies) {
                player.sendMessage(getMessage("enemy-target-limit-reached"));
                return;
            }

            clan.addEnemy(targetClan.getName());
            targetClan.addEnemy(clan.getName());
            player.sendMessage(getMessage("enemy-added").replace("%clan%", targetClan.getName()));

            // Notify target clan leader
            Player targetLeader = Bukkit.getPlayer(targetClan.getLeader());
            if (targetLeader != null && targetLeader.isOnline()) {
                targetLeader.sendMessage(getMessage("enemy-declared-by").replace("%clan%", clan.getName()));
            }
        }

        clanManager.saveClans();
    }

    private void handleAdmin(Player player, String[] args) {
        if (!player.hasPermission("cubeclans.admin")) {
            player.sendMessage(getMessage("no-permission"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ColorUtil.translate("&cUsage: /clan admin <disband|reload|resetkdr> [clan]"));
            return;
        }

        // /clan admin reload
        if (args[1].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("cubeclans.admin.reload")) {
                player.sendMessage(getMessage("no-permission"));
                return;
            }
            plugin.reloadConfig();
            plugin.reloadMessages();
            plugin.reloadRanks();
            player.sendMessage(ColorUtil.translate(plugin.getMessage("prefix") + " " + plugin.getMessage("config-reloaded")));
            return;
        }

        // /clan admin disband <clan>
        if (args[1].equalsIgnoreCase("disband") && args.length >= 3) {
            if (!player.hasPermission("cubeclans.admin.disband")) {
                player.sendMessage(getMessage("no-permission"));
                return;
            }
            String clanName = args[2];
            if (clanManager.deleteClan(clanName)) {
                player.sendMessage(getMessage("clan-disbanded-admin")
                        .replace("%clan%", clanName));
                clanManager.saveClans();
            } else {
                player.sendMessage(getMessage("clan-not-found"));
            }
            return;
        }

        // /clan admin resetkdr <clan>
        if (args[1].equalsIgnoreCase("resetkdr") && args.length >= 3) {
            if (!player.hasPermission("cubeclans.admin.resetkdr")) {
                player.sendMessage(getMessage("no-permission"));
                return;
            }
            String clanName = args[2];
            Clan targetClan = clanManager.getClan(clanName);
            if (targetClan == null) {
                player.sendMessage(getMessage("kdr-reset-clan-not-found").replace("%clan%", clanName));
                return;
            }
            targetClan.resetKDRStats();
            clanManager.saveClan(targetClan);
            player.sendMessage(getMessage("kdr-reset-success").replace("%clan%", targetClan.getName()));
            return;
        }
    }

    private void handleHelp(Player player, String[] args) {
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                player.sendMessage(ColorUtil.translate("&cUsage: /clan help <1|2>"));
                return;
            }
        }

        if (page < 1) page = 1;
        if (page > 2) page = 2;

        sendHelp(player, page);
    }

    private void sendHelp(Player player, int page) {
        player.sendMessage(ColorUtil.translate(getMessage("help-header")));
        String title = plugin.getMessage("help-title-page");
        if (title != null) {
            player.sendMessage(ColorUtil.translate(title.replace("%page%", String.valueOf(page))));
        } else {
            player.sendMessage(ColorUtil.translate(getMessage("help-title")));
        }
        player.sendMessage(ColorUtil.translate(getMessage("help-header")));

        if (page == 1) {
            player.sendMessage(ColorUtil.translate(getMessage("help-clan")));
            player.sendMessage(ColorUtil.translate(getMessage("help-create")));
            player.sendMessage(ColorUtil.translate(getMessage("help-disband")));
            player.sendMessage(ColorUtil.translate(getMessage("help-invite")));
            player.sendMessage(ColorUtil.translate(getMessage("help-accept")));
            player.sendMessage(ColorUtil.translate(getMessage("help-leave")));
            player.sendMessage(ColorUtil.translate(getMessage("help-kick")));
            player.sendMessage(ColorUtil.translate(getMessage("help-info")));
            player.sendMessage(ColorUtil.translate(getMessage("help-members")));
            player.sendMessage(ColorUtil.translate(getMessage("help-chat")));
            player.sendMessage(ColorUtil.translate(getMessage("help-chat-ally")));
            player.sendMessage(ColorUtil.translate(getMessage("help-pvp")));
            player.sendMessage(ColorUtil.translate(getMessage("help-list")));
        } else {
            player.sendMessage(ColorUtil.translate(getMessage("help-ally")));
            player.sendMessage(ColorUtil.translate(getMessage("help-ally-accept")));
            player.sendMessage(ColorUtil.translate(getMessage("help-ally-deny")));
            player.sendMessage(ColorUtil.translate(getMessage("help-ally-remove")));
            player.sendMessage(ColorUtil.translate(getMessage("help-ally-list")));
            player.sendMessage(ColorUtil.translate(getMessage("help-enemy")));
            player.sendMessage(ColorUtil.translate(getMessage("help-enemy-remove")));
            player.sendMessage(ColorUtil.translate(getMessage("help-enemy-list")));
            player.sendMessage(ColorUtil.translate(getMessage("help-bank-deposit")));
            player.sendMessage(ColorUtil.translate(getMessage("help-bank-withdraw")));
            player.sendMessage(ColorUtil.translate(getMessage("help-bank-balance")));
            if (player.hasPermission("cubeclans.admin")) {
                player.sendMessage(ColorUtil.translate(getMessage("help-admin-disband")));
                player.sendMessage(ColorUtil.translate(getMessage("help-admin-reload")));
                player.sendMessage(ColorUtil.translate(getMessage("help-admin-resetkdr")));
            }
        }

        player.sendMessage(ColorUtil.translate(getMessage("help-footer")));
    }

    private String getMessage(String key) {
        String prefix = ColorUtil.translate(plugin.getMessage("prefix"));
        String message = ColorUtil.translate(plugin.getMessage(key));
        return prefix + " " + message;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        Player player = (Player) sender;
        Clan playerClan = clanManager.getPlayerClan(player.getUniqueId());
        boolean inClan = playerClan != null;
        boolean isLeader = inClan && playerClan.isLeader(player.getUniqueId());
        boolean canInvite = inClan && player.hasPermission("cubeclans.invite")
                && playerClan.hasPermission(player.getUniqueId(), ClanPermission.INVITE, plugin);
        boolean canKick = inClan && player.hasPermission("cubeclans.kick")
                && playerClan.hasPermission(player.getUniqueId(), ClanPermission.KICK, plugin);
        boolean canPromote = inClan && player.hasPermission("cubeclans.promote")
                && playerClan.hasPermission(player.getUniqueId(), ClanPermission.PROMOTE, plugin);
        boolean canDemote = inClan && player.hasPermission("cubeclans.demote")
                && playerClan.hasPermission(player.getUniqueId(), ClanPermission.DEMOTE, plugin);
        boolean canAlly = inClan && player.hasPermission("cubeclans.ally")
                && playerClan.hasPermission(player.getUniqueId(), ClanPermission.ALLY, plugin);
        boolean canEnemy = inClan && player.hasPermission("cubeclans.enemy")
                && playerClan.hasPermission(player.getUniqueId(), ClanPermission.ENEMY, plugin);
        boolean canPvp = inClan && player.hasPermission("cubeclans.pvp")
                && playerClan.hasPermission(player.getUniqueId(), ClanPermission.PVP, plugin);
        boolean canChat = inClan && player.hasPermission("cubeclans.chat");
        boolean canBank = inClan && player.hasPermission("cubeclans.bank");
        String root = args[0].toLowerCase();

        if (args.length == 1) {
            List<String> rootCompletions = new ArrayList<>();

            if (!inClan && player.hasPermission("cubeclans.create")) rootCompletions.add("create");
            if (inClan && isLeader && player.hasPermission("cubeclans.disband")) {
                rootCompletions.add("disband");
                rootCompletions.add("delete");
            }
            if (canInvite) rootCompletions.add("invite");
            if (!inClan && player.hasPermission("cubeclans.accept")) {
                rootCompletions.add("accept");
                rootCompletions.add("join");
            }
            if (inClan && !isLeader && player.hasPermission("cubeclans.leave")) rootCompletions.add("leave");
            if (canKick) rootCompletions.add("kick");
            if (canPromote) rootCompletions.add("promote");
            if (canDemote) rootCompletions.add("demote");

            rootCompletions.add("info");
            if (inClan) rootCompletions.add("members");
            rootCompletions.add("help");

            if (canAlly) rootCompletions.add("ally");
            if (canEnemy) rootCompletions.add("enemy");
            if (canBank) rootCompletions.add("bank");
            if (player.hasPermission("cubeclans.list")) rootCompletions.add("list");
            if (canPvp) rootCompletions.add("pvp");
            if (canChat) rootCompletions.add("chat");

            if (player.hasPermission("cubeclans.admin")) rootCompletions.add("admin");

            return filterByPrefix(rootCompletions, args[0]);
        }

        if (args.length == 2) {
            switch (root) {
                case "invite":
                    if (!canInvite) return new ArrayList<>();
                    return filterByPrefix(getOnlinePlayerNamesNotInClan(player.getUniqueId()), args[1]);
                case "kick":
                    if (!canKick) return new ArrayList<>();
                    return filterByPrefix(getClanMemberNames(playerClan, player.getUniqueId()), args[1]);
                case "promote":
                    if (!canPromote) return new ArrayList<>();
                    return filterByPrefix(getClanMemberNames(playerClan, player.getUniqueId()), args[1]);
                case "demote":
                    if (!canDemote) return new ArrayList<>();
                    return filterByPrefix(getClanMemberNames(playerClan, player.getUniqueId()), args[1]);
                case "accept":
                case "join":
                    if (inClan || !player.hasPermission("cubeclans.accept")) return new ArrayList<>();
                    return filterByPrefix(getInvitedClanNames(player.getUniqueId()), args[1]);
                case "info":
                    return filterByPrefix(clanManager.getAllClanNames(), args[1]);
                case "bank":
                    if (!canBank) return new ArrayList<>();
                    List<String> bankCompletions = new ArrayList<>();
                    if (playerClan.hasPermission(player.getUniqueId(), ClanPermission.BANK_DEPOSIT, plugin)) {
                        bankCompletions.add("deposit");
                    }
                    if (playerClan.hasPermission(player.getUniqueId(), ClanPermission.BANK_WITHDRAW, plugin)) {
                        bankCompletions.add("withdraw");
                    }
                    if (playerClan.hasPermission(player.getUniqueId(), ClanPermission.BANK_BALANCE, plugin)) {
                        bankCompletions.add("balance");
                    }
                    return filterByPrefix(bankCompletions, args[1]);
                case "pvp":
                    if (!canPvp) return new ArrayList<>();
                    return filterByPrefix(Arrays.asList("on", "off", "toggle"), args[1]);
                case "chat":
                    if (!canChat) return new ArrayList<>();
                    List<String> chatCompletions = new ArrayList<>(Arrays.asList("on", "off", "toggle"));
                    if (playerClan != null && !playerClan.getAllies().isEmpty()) {
                        chatCompletions.add("ally");
                    }
                    return filterByPrefix(chatCompletions, args[1]);
                case "help":
                    return filterByPrefix(Arrays.asList("1", "2"), args[1]);
                case "ally": {
                    if (!canAlly || playerClan == null) return new ArrayList<>();
                    List<String> allyCompletions = new ArrayList<>();
                    allyCompletions.add("list");
                    if (!playerClan.getAllyRequests().isEmpty()) {
                        allyCompletions.add("accept");
                        allyCompletions.add("deny");
                    }
                    if (!playerClan.getAllies().isEmpty()) allyCompletions.add("remove");
                    for (String clanName : clanManager.getAllClanNames()) {
                        if (!clanName.equalsIgnoreCase(playerClan.getName())
                                && !playerClan.isAlly(clanName)
                                && !playerClan.hasAllyRequest(clanName)) {
                            allyCompletions.add(clanName);
                        }
                    }
                    return filterByPrefix(allyCompletions, args[1]);
                }
                case "enemy": {
                    if (!canEnemy || playerClan == null) return new ArrayList<>();
                    List<String> enemyCompletions = new ArrayList<>();
                    enemyCompletions.add("list");
                    if (!playerClan.getEnemies().isEmpty()) enemyCompletions.add("remove");
                    for (String clanName : clanManager.getAllClanNames()) {
                        if (!clanName.equalsIgnoreCase(playerClan.getName())) {
                            enemyCompletions.add(clanName);
                        }
                    }
                    return filterByPrefix(enemyCompletions, args[1]);
                }
                case "admin": {
                    if (!player.hasPermission("cubeclans.admin")) return new ArrayList<>();
                    List<String> adminCompletions = new ArrayList<>();
                    if (player.hasPermission("cubeclans.admin.disband")) adminCompletions.add("disband");
                    if (player.hasPermission("cubeclans.admin.reload")) adminCompletions.add("reload");
                    if (player.hasPermission("cubeclans.admin.resetkdr")) adminCompletions.add("resetkdr");
                    return filterByPrefix(adminCompletions, args[1]);
                }
                default:
                    return new ArrayList<>();
            }
        }

        if (args.length == 3) {
            if (root.equals("chat") && args[1].equalsIgnoreCase("ally")) {
                if (!canChat || playerClan == null || playerClan.getAllies().isEmpty()) return new ArrayList<>();
                return filterByPrefix(Arrays.asList("on", "off", "toggle"), args[2]);
            }

            if (root.equals("ally") && playerClan != null) {
                if (!canAlly) return new ArrayList<>();
                if (args[1].equalsIgnoreCase("accept") || args[1].equalsIgnoreCase("deny")) {
                    return filterByPrefix(new ArrayList<>(playerClan.getAllyRequests()), args[2]);
                }
                if (args[1].equalsIgnoreCase("remove")) {
                    return filterByPrefix(new ArrayList<>(playerClan.getAllies()), args[2]);
                }
            }

            if (root.equals("enemy") && args[1].equalsIgnoreCase("remove") && playerClan != null) {
                if (!canEnemy) return new ArrayList<>();
                return filterByPrefix(new ArrayList<>(playerClan.getEnemies()), args[2]);
            }

            if (root.equals("admin") && (args[1].equalsIgnoreCase("disband") || args[1].equalsIgnoreCase("resetkdr"))) {
                if (!player.hasPermission("cubeclans.admin")) return new ArrayList<>();
                return filterByPrefix(clanManager.getAllClanNames(), args[2]);
            }
        }

        return new ArrayList<>();
    }

    private List<String> getOnlinePlayerNamesNotInClan(UUID exclude) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getUniqueId)
                .filter(uuid -> !uuid.equals(exclude))
                .filter(uuid -> !clanManager.isInClan(uuid))
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    private List<String> getInvitedClanNames(UUID playerId) {
        List<String> invited = new ArrayList<>();
        for (String clanName : clanManager.getAllClanNames()) {
            if (clanManager.hasInvitation(playerId, clanName)) {
                invited.add(clanName);
            }
        }
        return invited;
    }

    private List<String> getClanMemberNames(Clan clan, UUID exclude) {
        if (clan == null) {
            return new ArrayList<>();
        }

        List<String> names = new ArrayList<>();
        for (UUID memberId : clan.getMembers()) {
            if (memberId.equals(exclude)) {
                continue;
            }
            String name = Bukkit.getOfflinePlayer(memberId).getName();
            if (name != null && !name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    private List<String> filterByPrefix(List<String> candidates, String input) {
        String lowerInput = input.toLowerCase();
        return candidates.stream()
                .filter(Objects::nonNull)
                .distinct()
                .filter(value -> value.toLowerCase().startsWith(lowerInput))
                .collect(Collectors.toList());
    }

    private void handleBank(Player player, String[] args) {
        if (!player.hasPermission("cubeclans.bank")) {
            player.sendMessage(getMessage("no-permission"));
            return;
        }

        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(getMessage("not-in-clan"));
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(ColorUtil.translate("&cUsage: /clan bank <deposit|withdraw|balance> [amount]"));
            return;
        }
        String action = args[1].toLowerCase();
        com.cubeclans.economy.VaultHook vault = plugin.getVaultHook();
        if (!vault.isEnabled()) {
            player.sendMessage(getMessage("bank-vault-missing"));
            return;
        }
        switch (action) {
            case "deposit":
                // Check clan permission
                if (!clan.hasPermission(player.getUniqueId(), com.cubeclans.models.ClanPermission.BANK_DEPOSIT, plugin)) {
                    player.sendMessage(getMessage("no-bank-permission"));
                    return;
                }
                if (args.length < 3) {
                    player.sendMessage(ColorUtil.translate("&cUsage: /clan bank deposit <amount>"));
                    return;
                }
                double deposit;
                try {
                    deposit = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(getMessage("bank-invalid-amount"));
                    return;
                }
                if (deposit <= 0) {
                    player.sendMessage(getMessage("bank-amount-positive"));
                    return;
                }
                if (vault.getEconomy().getBalance(player) < deposit) {
                    player.sendMessage(getMessage("bank-not-enough-player"));
                    return;
                }
                vault.getEconomy().withdrawPlayer(player, deposit);
                clan.depositBank(deposit);
                clanManager.saveClans();
                player.sendMessage(getMessage("bank-deposit-success")
                    .replace("%amount%", String.format("%.2f", deposit))
                    .replace("%balance%", String.format("%.2f", clan.getBankBalance())));
                break;
            case "withdraw":
                // Check clan permission
                if (!clan.hasPermission(player.getUniqueId(), com.cubeclans.models.ClanPermission.BANK_WITHDRAW, plugin)) {
                    player.sendMessage(getMessage("no-bank-permission"));
                    return;
                }
                if (args.length < 3) {
                    player.sendMessage(ColorUtil.translate("&cUsage: /clan bank withdraw <amount>"));
                    return;
                }
                double withdraw;
                try {
                    withdraw = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(getMessage("bank-invalid-amount"));
                    return;
                }
                if (withdraw <= 0) {
                    player.sendMessage(getMessage("bank-amount-positive"));
                    return;
                }
                if (!clan.withdrawBank(withdraw)) {
                    player.sendMessage(getMessage("bank-not-enough-clan"));
                    return;
                }
                vault.getEconomy().depositPlayer(player, withdraw);
                clanManager.saveClans();
                player.sendMessage(getMessage("bank-withdraw-success")
                    .replace("%amount%", String.format("%.2f", withdraw))
                    .replace("%balance%", String.format("%.2f", clan.getBankBalance())));
                break;
            case "balance":
                // Check clan permission
                if (!clan.hasPermission(player.getUniqueId(), com.cubeclans.models.ClanPermission.BANK_BALANCE, plugin)) {
                    player.sendMessage(getMessage("no-bank-permission"));
                    return;
                }
                player.sendMessage(getMessage("bank-balance")
                    .replace("%balance%", String.format("%.2f", clan.getBankBalance())));
                break;
            default:
                player.sendMessage(ColorUtil.translate("&cUsage: /clan bank <deposit|withdraw|balance> [amount]"));
                break;
        }
    }

    private void handleList(Player player) {
        if (!player.hasPermission("cubeclans.list")) {
            player.sendMessage(getMessage("no-permission"));
            return;
        }
        new com.cubeclans.menus.ClanMenu(plugin, player).openClanListMenu();
    }

    private void handleChatToggle(Player player, String[] args) {
        if (!player.hasPermission("cubeclans.chat")) {
            player.sendMessage(getMessage("no-permission"));
            return;
        }

        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(getMessage("not-in-clan"));
            return;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("ally")) {
            handleAllyChatToggle(player, clan, args);
            return;
        }

        boolean current = clanManager.isClanChatEnabled(player.getUniqueId());
        Boolean desired = null;
        if (args.length >= 2) {
            String arg = args[1].toLowerCase();
            if (arg.equals("on") || arg.equals("enable") || arg.equals("enabled") || arg.equals("true")) {
                desired = true;
            } else if (arg.equals("off") || arg.equals("disable") || arg.equals("disabled") || arg.equals("false")) {
                desired = false;
            } else if (arg.equals("toggle")) {
                desired = !current;
            } else {
                player.sendMessage(ColorUtil.translate("&cUsage: /clan chat <on|off|toggle|ally>"));
                return;
            }
        }

        boolean newState = desired != null ? desired : !current;
        clanManager.setClanChat(player.getUniqueId(), newState);
        String messageKey = newState ? "clan-chat-enabled" : "clan-chat-disabled";
        player.sendMessage(getMessage(messageKey));
    }

    private void handleAllyChatToggle(Player player, Clan clan, String[] args) {
        if (clan.getAllies().isEmpty()) {
            player.sendMessage(getMessage("clan-ally-chat-no-allies"));
            return;
        }

        boolean current = clanManager.isAllyChatEnabled(player.getUniqueId());
        Boolean desired = null;
        if (args.length >= 3) {
            String arg = args[2].toLowerCase();
            if (arg.equals("on") || arg.equals("enable") || arg.equals("enabled") || arg.equals("true")) {
                desired = true;
            } else if (arg.equals("off") || arg.equals("disable") || arg.equals("disabled") || arg.equals("false")) {
                desired = false;
            } else if (arg.equals("toggle")) {
                desired = !current;
            } else {
                player.sendMessage(ColorUtil.translate("&cUsage: /clan chat ally <on|off|toggle>"));
                return;
            }
        }

        boolean newState = desired != null ? desired : !current;
        clanManager.setAllyChat(player.getUniqueId(), newState);
        String messageKey = newState ? "clan-ally-chat-enabled" : "clan-ally-chat-disabled";
        player.sendMessage(getMessage(messageKey));
    }

    private void handlePvp(Player player, String[] args) {
        if (!player.hasPermission("cubeclans.pvp")) {
            player.sendMessage(getMessage("no-permission"));
            return;
        }

        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(getMessage("not-in-clan"));
            return;
        }

        // Check clan permission
        if (!clan.hasPermission(player.getUniqueId(), com.cubeclans.models.ClanPermission.PVP, plugin)) {
            player.sendMessage(getMessage("clan-not-leader"));
            return;
        }

        Boolean desiredState = null;
        if (args.length >= 2) {
            String arg = args[1].toLowerCase();
            if (arg.equals("on") || arg.equals("enable") || arg.equals("enabled") || arg.equals("true")) {
                desiredState = true;
            } else if (arg.equals("off") || arg.equals("disable") || arg.equals("disabled") || arg.equals("false")) {
                desiredState = false;
            } else if (arg.equals("toggle")) {
                desiredState = !clan.isFriendlyFireEnabled();
            } else {
                player.sendMessage(ColorUtil.translate("&cUsage: /clan pvp <on|off|toggle>"));
                return;
            }
        }

        boolean newState = desiredState != null ? desiredState : !clan.isFriendlyFireEnabled();
        clan.setFriendlyFireEnabled(newState);
        clanManager.saveClan(clan);

        String messageKey = newState ? "clan-pvp-enabled" : "clan-pvp-disabled";
        String chatMessage = getMessage(messageKey);
        String titleMessage = ColorUtil.translate(plugin.getMessage(messageKey));

        for (UUID memberId : clan.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(chatMessage);
                sendTitleSafe(member, titleMessage);
            }
        }
    }

    private void sendTitleSafe(Player target, String title) {
        try {
            target.sendTitle(title, "", 10, 40, 10);
        } catch (NoSuchMethodError e) {
            try {
                target.sendTitle(title, "");
            } catch (Exception ignored) {
                target.sendMessage(title);
            }
        } catch (Exception ex) {
            target.sendMessage(title);
        }
    }
}

