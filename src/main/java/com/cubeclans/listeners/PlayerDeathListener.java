package com.cubeclans.listeners;

import com.cubeclans.CubeClans;
import com.cubeclans.managers.ClanManager;
import com.cubeclans.models.Clan;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final ClanManager clanManager;

    public PlayerDeathListener(CubeClans plugin) {
        this.clanManager = plugin.getClanManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Only count if killed by another player
        if (killer == null) {
            return;
        }

        // Check if both players are in clans
        Clan victimClan = clanManager.getPlayerClan(victim.getUniqueId());
        Clan killerClan = clanManager.getPlayerClan(killer.getUniqueId());

        // Only count statistics if both players are currently in a clan
        if (victimClan != null && killerClan != null) {
            // Add death to victim
            victimClan.addMemberDeath(victim.getUniqueId());
            clanManager.saveClan(victimClan);

            // Add kill to killer
            killerClan.addMemberKill(killer.getUniqueId());
            clanManager.saveClan(killerClan);
        }
    }
}
