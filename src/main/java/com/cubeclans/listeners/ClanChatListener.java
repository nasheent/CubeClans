package com.cubeclans.listeners;

import com.cubeclans.CubeClans;
import com.cubeclans.models.Clan;
import com.cubeclans.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class ClanChatListener implements Listener {

    private final CubeClans plugin;

    public ClanChatListener(CubeClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClanChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (plugin.getClanManager().isAllyChatEnabled(uuid)) {
            Clan clan = plugin.getClanManager().getPlayerClan(uuid);
            if (clan == null) {
                plugin.getClanManager().setAllyChat(uuid, false);
                return;
            }

            event.setCancelled(true);
            String rawMessage = event.getMessage();
            String format = plugin.getMessage("clan-ally-chat-format");
            String rendered = ColorUtil.translate(format
                    .replace("%player%", player.getName())
                    .replace("%clan%", clan.getName())
                    .replace("%message%", rawMessage));

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(rendered);

                for (String allyClanName : clan.getAllies()) {
                    Clan allyClan = plugin.getClanManager().getClan(allyClanName);
                    if (allyClan == null) {
                        continue;
                    }

                    for (UUID memberId : allyClan.getMembers()) {
                        Player member = Bukkit.getPlayer(memberId);
                        if (member != null && member.isOnline()) {
                            member.sendMessage(rendered);
                        }
                    }
                }
            });
            return;
        }

        if (!plugin.getClanManager().isClanChatEnabled(uuid)) {
            return;
        }

        Clan clan = plugin.getClanManager().getPlayerClan(uuid);
        if (clan == null) {
            plugin.getClanManager().setClanChat(uuid, false);
            return;
        }

        event.setCancelled(true);
        String rawMessage = event.getMessage();
        String format = plugin.getMessage("clan-chat-format");
        String rendered = ColorUtil.translate(format
                .replace("%player%", player.getName())
                .replace("%clan%", clan.getName())
                .replace("%message%", rawMessage));

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (UUID memberId : clan.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    member.sendMessage(rendered);
                }
            }
        });
    }
}
