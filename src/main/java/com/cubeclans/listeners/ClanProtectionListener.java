package com.cubeclans.listeners;

import com.cubeclans.CubeClans;
import com.cubeclans.models.Clan;
import com.cubeclans.utils.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ClanProtectionListener implements Listener {

    private final CubeClans plugin;

    public ClanProtectionListener(CubeClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        // Check if both entities are players
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        // Get clans for both players
        Clan victimClan = plugin.getClanManager().getPlayerClan(victim.getUniqueId());
        Clan attackerClan = plugin.getClanManager().getPlayerClan(attacker.getUniqueId());

        // If both players are in clans and they're in the same clan, cancel damage
        if (victimClan != null && attackerClan != null) {
            if (victimClan.getName().equalsIgnoreCase(attackerClan.getName())) {
                if (!victimClan.isFriendlyFireEnabled()) {
                    event.setCancelled(true);
                    attacker.sendMessage(getMessage("pvp-not-allowed"));
                }
            }
        }
    }

    private String getMessage(String key) {
        String prefix = ColorUtil.translate(plugin.getMessage("prefix"));

        String message = ColorUtil.translate(plugin.getMessage(key));
        return prefix + " " + message;
    }
}
