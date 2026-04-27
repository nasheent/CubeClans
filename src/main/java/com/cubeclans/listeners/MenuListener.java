package com.cubeclans.listeners;

import com.cubeclans.CubeClans;
import com.cubeclans.managers.ClanManager;
import com.cubeclans.menus.ClanMenu;
import com.cubeclans.models.Clan;
import com.cubeclans.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MenuListener implements Listener {

    private final CubeClans plugin;
    private final ClanManager clanManager;
    private final Map<UUID, String> awaitingInput;
    private final Map<UUID, Long> colorChangeCooldown;
    private final Map<UUID, Integer> membersPageMap;
    private final Map<UUID, Integer> clanListPageMap;

    public MenuListener(CubeClans plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.awaitingInput = new HashMap<>();
        this.colorChangeCooldown = new HashMap<>();
        this.membersPageMap = new HashMap<>();
        this.clanListPageMap = new HashMap<>();
    }

    /**
     * Get item type from PDC if available, otherwise use slot-based detection for 1.8.x
     */
    private String getItemType(ItemStack item, int slot, String menuType) {
        // Try PDC first (1.13+)
        String pdcType = com.cubeclans.utils.ItemBuilder.getMenuItemType(item);
        if (pdcType != null) {
            return pdcType;
        }

        // Fallback to slot-based detection for 1.8.x
        // This is when NamespacedKey is not available
        if (!com.cubeclans.utils.VersionHelper.hasNamespacedKey()) {
            return getItemTypeBySlot(slot, menuType);
        }

        return null;
    }

    /**
     * Detect item type based on slot for versions without PDC support
     */
    private String getItemTypeBySlot(int slot, String menuType) {
        switch (menuType) {
            case "main": {
                org.bukkit.configuration.ConfigurationSection mainItems = plugin.getConfig().getConfigurationSection("menu.main.items");
                if (mainItems != null) {
                    int createSlot = mainItems.getConfigurationSection("create-clan") != null ?
                            mainItems.getConfigurationSection("create-clan").getInt("slot", 22) : -1;
                    int detailsSlot = mainItems.getConfigurationSection("clan-details") != null ?
                            mainItems.getConfigurationSection("clan-details").getInt("slot", 20) : -1;
                    int listSlot = mainItems.getConfigurationSection("clan-list") != null ?
                            mainItems.getConfigurationSection("clan-list").getInt("slot", 22) : -1;

                    // If slots differ, map directly
                    if (slot == createSlot && createSlot != listSlot) return "create-clan";
                    if (slot == listSlot && createSlot != listSlot) return "clan-list";
                    if (slot == detailsSlot) return "clan-details";

                    // If slots are equal (default 22), return ambiguous type to resolve later
                    if (createSlot == listSlot && slot == createSlot) return "create-or-list";
                }
                break;
            }
            case "members": {
                org.bukkit.configuration.ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("menu.members");
                if (cfg != null) {
                    int backSlot = cfg.getConfigurationSection("back") != null ? cfg.getConfigurationSection("back").getInt("slot", 49) : -1;
                    int prevSlot = cfg.getConfigurationSection("previous-page") != null ? cfg.getConfigurationSection("previous-page").getInt("slot", 45) : -1;
                    int nextSlot = cfg.getConfigurationSection("next-page") != null ? cfg.getConfigurationSection("next-page").getInt("slot", 53) : -1;
                    if (slot == backSlot) return "back";
                    if (slot == prevSlot) return "previous-page";
                    if (slot == nextSlot) return "next-page";
                }
                break;
            }
            case "clan-list": {
                org.bukkit.configuration.ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("clan-list");
                if (cfg == null) cfg = plugin.getConfig().getConfigurationSection("menu.clan-list");
                if (cfg != null) {
                    int backSlot = cfg.getConfigurationSection("back") != null ? cfg.getConfigurationSection("back").getInt("slot", 49) : -1;
                    int prevSlot = cfg.getConfigurationSection("previous-page") != null ? cfg.getConfigurationSection("previous-page").getInt("slot", 45) : -1;
                    int nextSlot = cfg.getConfigurationSection("next-page") != null ? cfg.getConfigurationSection("next-page").getInt("slot", 53) : -1;
                    if (slot == backSlot) return "back";
                    if (slot == prevSlot) return "previous-page";
                    if (slot == nextSlot) return "next-page";
                }
                break;
            }
            case "clan-details": {
                org.bukkit.configuration.ConfigurationSection items = plugin.getConfig().getConfigurationSection("menu.clan-details.items");
                if (items != null) {
                    int backSlot = items.getConfigurationSection("back") != null ? items.getConfigurationSection("back").getInt("slot", 40) : -1;
                    int membersSlot = items.getConfigurationSection("members") != null ? items.getConfigurationSection("members").getInt("slot", 22) : -1;
                    int colorSlot = items.getConfigurationSection("color") != null ? items.getConfigurationSection("color").getInt("slot", 24) : -1;
                    if (slot == backSlot) return "back";
                    if (slot == membersSlot) return "members";
                    if (slot == colorSlot) return "color";
                }
                break;
            }
        }
        return null;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        String strippedTitle = ColorUtil.strip(title);
        
        // Check if it's a clan menu
        String mainMenuTitleStripped = ColorUtil.strip(plugin.getConfig().getString("menu.main.title", "&8Clan Menu"));
        String membersTitleStripped = ColorUtil.strip(plugin.getConfig().getString("menu.members.title", "&8Clan Members"));
        String colorMenuTitleStripped = ColorUtil.strip(plugin.getConfig().getString("menu.color-selector.title", "&8Clan Color"));
        String clanListTitleStripped = ColorUtil.strip(plugin.getConfig().getString("menu.clan-list.title", "&8Clan list"));
        String clanDetailsTitleStripped = ColorUtil.strip(plugin.getConfig().getString("menu.clan-details.title", "&8Clan Details"));
        
        if (!strippedTitle.equals(mainMenuTitleStripped) && 
            !strippedTitle.equals(membersTitleStripped) &&
            !strippedTitle.equals(colorMenuTitleStripped) &&
            !strippedTitle.equals(clanListTitleStripped) &&
            !strippedTitle.equals(clanDetailsTitleStripped)) {
            return;
        }
        
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Handle main menu clicks
        if (strippedTitle.equals(mainMenuTitleStripped)) {
            handleMainMenuClick(player, clicked, event.getSlot());
        }
        // Handle members menu clicks
        else if (strippedTitle.equals(membersTitleStripped)) {
            handleMembersClick(player, clicked, event.getSlot());
        }
        // Handle color selector menu
        else if (strippedTitle.equals(colorMenuTitleStripped)) {
            handleColorMenuClick(player, clicked, event.getSlot());
        }
        // Handle clan list menu
        else if (strippedTitle.equals(clanListTitleStripped)) {
            handleClanListClick(player, clicked, event.getSlot());
        }
        // Handle clan details menu
        else if (strippedTitle.equals(clanDetailsTitleStripped)) {
            handleClanDetailsClick(player, clicked, event.getSlot());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        String title = event.getView().getTitle();
        String strippedTitle = ColorUtil.strip(title);

        String mainMenuTitleStripped = ColorUtil.strip(plugin.getConfig().getString("menu.main.title", "&8Clan Menu"));
        String membersTitleStripped = ColorUtil.strip(plugin.getConfig().getString("menu.members.title", "&8Clan Members"));
        String colorMenuTitleStripped = ColorUtil.strip(plugin.getConfig().getString("menu.color-selector.title", "&8Clan Color"));
        String clanListTitleStripped = ColorUtil.strip(plugin.getConfig().getString("menu.clan-list.title", "&8Clan list"));
        String clanDetailsTitleStripped = ColorUtil.strip(plugin.getConfig().getString("menu.clan-details.title", "&8Clan Details"));

        if (strippedTitle.equals(mainMenuTitleStripped) ||
            strippedTitle.equals(membersTitleStripped) ||
            strippedTitle.equals(colorMenuTitleStripped) ||
            strippedTitle.equals(clanListTitleStripped) ||
            strippedTitle.equals(clanDetailsTitleStripped)) {
            event.setCancelled(true);
        }
    }

    private void handleColorMenuClick(Player player, ItemStack item, int slot) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) return;

        // Check clan permission
        if (!clan.hasPermission(player.getUniqueId(), com.cubeclans.models.ClanPermission.COLORS, plugin)) {
            player.sendMessage(ColorUtil.translate(
                plugin.getMessage("prefix") + 
                " " + plugin.getMessage("clan-not-leader")));
            return;
        }

        // Back item by config - returns to clan details menu
        org.bukkit.configuration.ConfigurationSection back = plugin.getConfig().getConfigurationSection("menu.color-selector.back");
        if (back != null) {
            int backSlot = back.getInt("slot", 31);
            if (slot == backSlot) {
                Clan backClan = clanManager.getPlayerClan(player.getUniqueId());
                if (backClan != null) {
                    new ClanMenu(plugin, player).openClanDetailsMenu();
                } else {
                    new ClanMenu(plugin, player).openMainMenu();
                }
                return;
            }
        }

        // Check cooldown (configurable)
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        int cooldownSeconds = plugin.getConfig().getInt("settings.color-change-cooldown", 3);
        long cooldownMillis = cooldownSeconds * 1000L;
        
        if (colorChangeCooldown.containsKey(playerUUID)) {
            long lastChange = colorChangeCooldown.get(playerUUID);
            long timePassed = currentTime - lastChange;
            if (timePassed < cooldownMillis) {
                long remainingSeconds = (cooldownMillis - timePassed) / 1000 + 1;
                String cooldownMsg = plugin.getMessage("color-change-cooldown");
                player.sendMessage(ColorUtil.translate(
                    plugin.getMessage("prefix") + " " + 
                    cooldownMsg.replace("%time%", String.valueOf(remainingSeconds))));
                return;
            }
        }

        // Read colors from config and match by slot
        java.util.List<java.util.Map<?, ?>> colors = plugin.getConfig().getMapList("menu.color-selector.colors");
        for (java.util.Map<?, ?> rawEntry : colors) {
            java.util.Map<String, Object> entry = new java.util.HashMap<>();
            for (java.util.Map.Entry<?, ?> e : rawEntry.entrySet()) entry.put(String.valueOf(e.getKey()), e.getValue());
            int confSlot = (int) entry.getOrDefault("slot", -1);
            if (confSlot != slot) continue;
            String code = String.valueOf(entry.getOrDefault("code", "&f"));
            String displayName = String.valueOf(entry.getOrDefault("name", "&fColor"));
            clan.setColorCode(code);
            clanManager.saveClans();
            
            // Update cooldown
            colorChangeCooldown.put(playerUUID, currentTime);
            
            player.sendMessage(ColorUtil.translate(
                plugin.getMessage("prefix") + " " +
                plugin.getMessage("clan-color-updated")
                    .replace("%color%", ColorUtil.translate(displayName))));
            // Reopen the color menu to allow trying other colors
            new ClanMenu(plugin, player).openColorMenu(clan);
            break;
        }
    }

    private void handleMainMenuClick(Player player, ItemStack item, int slot) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        
        // Get item type from PDC marker o fallback por slot
        String itemType = getItemType(item, slot, "main");
        // Resolver ambigüedad cuando create-clan y clan-list comparten slot
        if (!com.cubeclans.utils.VersionHelper.hasNamespacedKey() && "create-or-list".equals(itemType)) {
            itemType = clanManager.isInClan(player.getUniqueId()) ? "clan-list" : "create-clan";
        }
        if (itemType == null) return;
        
        // Create Clan
        if (itemType.equals("create-clan")) {
            player.closeInventory();
            player.sendMessage(ColorUtil.translate(
                plugin.getMessage("prefix") + 
                " " + plugin.getMessage("clan-create-prompt")));
            awaitingInput.put(player.getUniqueId(), "create");
        }
        // Clan Details
        else if (itemType.equals("clan-details")) {
            Clan clan = clanManager.getPlayerClan(player.getUniqueId());
            if (clan != null) {
                new ClanMenu(plugin, player).openClanDetailsMenu();
            } else {
                player.sendMessage(ColorUtil.translate(plugin.getMessage("prefix") + " " + plugin.getMessage("not-in-clan")));
            }
        }
        // Invite Player
        // (Ya no hay botón de invite en el main según config actual)
        // Clan List
        else if (itemType.equals("clan-list")) {
            player.closeInventory();
            clanListPageMap.put(player.getUniqueId(), 0);
            new ClanMenu(plugin, player).openClanListMenu(0);
        }
    }

    private void handleMembersClick(Player player, ItemStack item, int slot) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        
        String itemType = getItemType(item, slot, "members");
        if (itemType == null) return;
        
        UUID uuid = player.getUniqueId();
        Clan clan = clanManager.getPlayerClan(uuid);
        if (clan == null) {
            player.closeInventory();
            return;
        }

        // Back button
        if (itemType.equals("back")) {
            membersPageMap.remove(uuid);
            new ClanMenu(plugin, player).openClanDetailsMenu();
            return;
        }

        // Next page
        if (itemType.equals("next-page")) {
            int current = membersPageMap.getOrDefault(uuid, 0);
            int totalMembers = clan.getMembers().size();
            int perPage = 21;
            int maxPage = (int) Math.ceil(totalMembers / (double) perPage) - 1;
            if (current < maxPage) {
                current++;
                membersPageMap.put(uuid, current);
                new ClanMenu(plugin, player).openMembersMenu(clan, current);
            }
            return;
        }

        // Previous page
        if (itemType.equals("previous-page")) {
            int current = membersPageMap.getOrDefault(uuid, 0);
            if (current > 0) {
                current--;
                membersPageMap.put(uuid, current);
                new ClanMenu(plugin, player).openMembersMenu(clan, current);
            }
        }
    }

    private void handleClanListClick(Player player, ItemStack item, int slot) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        
        String itemType = getItemType(item, slot, "clan-list");
        if (itemType == null) return;
        
        UUID uuid = player.getUniqueId();

        // Back button
        if (itemType.equals("back")) {
            clanListPageMap.remove(uuid);
            if (clanManager.isInClan(uuid)) new ClanMenu(plugin, player).openMainMenu(); else player.closeInventory();
            return;
        }

        // Next page
        if (itemType.equals("next-page")) {
            int current = clanListPageMap.getOrDefault(uuid, 0);
            int totalClans = plugin.getClanManager().getAllClans().size();
            int perPage = 21;
            int maxPage = (int) Math.ceil(totalClans / (double) perPage) - 1;
            if (current < maxPage) {
                current++;
                clanListPageMap.put(uuid, current);
                new ClanMenu(plugin, player).openClanListMenu(current);
            }
            return;
        }

        // Previous page
        if (itemType.equals("previous-page")) {
            int current = clanListPageMap.getOrDefault(uuid, 0);
            if (current > 0) {
                current--;
                clanListPageMap.put(uuid, current);
                new ClanMenu(plugin, player).openClanListMenu(current);
            }
        }
    }

    private void handleClanDetailsClick(Player player, ItemStack item, int slot) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String itemType = getItemType(item, slot, "clan-details");
        if (itemType == null) return;

        Clan clan = clanManager.getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.closeInventory();
            return;
        }

        // Back button
        if (itemType.equals("back")) {
            new ClanMenu(plugin, player).openMainMenu();
            return;
        }

        // Members item
        if (itemType.equals("members")) {
            membersPageMap.put(player.getUniqueId(), 0);
            new ClanMenu(plugin, player).openMembersMenu(clan, 0);
            return;
        }

        // Color item
        if (itemType.equals("color")) {
            new ClanMenu(plugin, player).openColorMenu(clan);
            return;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (!awaitingInput.containsKey(uuid)) return;
        
        event.setCancelled(true);
        String inputType = awaitingInput.remove(uuid);
        String input = ColorUtil.strip(event.getMessage()).trim();
        
        if (inputType.equals("create")) {
            // Check if player wants to cancel FIRST
            if (input.equalsIgnoreCase("cancel")) {
                final Player finalPlayer = player;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    String prefix = plugin.getMessage("prefix");
                    String cancelMsg = plugin.getMessage("clan-creation-cancelled");
                    finalPlayer.sendMessage(ColorUtil.translate(prefix + " " + cancelMsg));
                }, 1L);
                return;
            }
            
            // Run on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (clanManager.isInClan(uuid)) {
                    player.sendMessage(ColorUtil.translate(
                        plugin.getMessage("prefix") + 
                        " " + plugin.getMessage("already-in-clan")));
                    return;
                }
                
                int minLength = plugin.getConfig().getInt("settings.min-clan-name-length", 3);
                int maxLength = plugin.getConfig().getInt("settings.max-clan-name-length", 16);
                
                if (input.length() < minLength) {
                    player.sendMessage(ColorUtil.translate(
                        plugin.getMessage("prefix") + 
                        " " + plugin.getMessage("clan-name-too-short")
                            .replace("%min%", String.valueOf(minLength))));
                    return;
                }
                
                if (input.length() > maxLength) {
                    player.sendMessage(ColorUtil.translate(
                        plugin.getMessage("prefix") + 
                        " " + plugin.getMessage("clan-name-too-long")
                            .replace("%max%", String.valueOf(maxLength))));
                    return;
                }
                
                if (clanManager.clanExists(input)) {
                    player.sendMessage(ColorUtil.translate(
                        plugin.getMessage("prefix") + 
                        " " + plugin.getMessage("clan-already-exists")));
                    return;
                }
                
                // Strip color codes from clan name
                String cleanName = ColorUtil.strip(input);
                Clan clan = clanManager.createClan(cleanName, uuid);
                if (clan != null) {
                    player.sendMessage(ColorUtil.translate(
                        plugin.getMessage("prefix") + 
                        " " + plugin.getMessage("clan-created")
                            .replace("%clan%", clan.getName())));
                    clanManager.saveClans();
                }
            });
        }
        else if (inputType.equals("invite")) {
            // Check if player wants to cancel
            if (input.equalsIgnoreCase("cancel")) {
                final Player finalPlayer = player;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    String prefix = plugin.getMessage("prefix");
                    String cancelMsg = plugin.getMessage("clan-creation-cancelled");
                    finalPlayer.sendMessage(ColorUtil.translate(prefix + " " + cancelMsg));
                }, 1L);
                return;
            }
            
            // Run on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                Clan clan = clanManager.getPlayerClan(uuid);
                if (clan == null || !clan.isLeader(uuid)) {
                    player.sendMessage(ColorUtil.translate(
                        plugin.getMessage("prefix") + 
                        " " + plugin.getMessage("clan-not-leader")));
                    return;
                }
                
                Player target = Bukkit.getPlayer(input);
                if (target == null) {
                    player.sendMessage(ColorUtil.translate(
                        plugin.getMessage("prefix") + 
                        " " + plugin.getMessage("player-not-found")));
                    return;
                }
                
                if (clanManager.isInClan(target.getUniqueId())) {
                    player.sendMessage(ColorUtil.translate(
                        plugin.getMessage("prefix") + 
                        " " + plugin.getMessage("already-in-clan-other")));
                    return;
                }
                
                int maxMembers = plugin.getConfig().getInt("settings.max-clan-members", 10);
                if (clan.getMemberCount() >= maxMembers) {
                    player.sendMessage(ColorUtil.translate(
                        plugin.getMessage("prefix") + 
                        " " + plugin.getMessage("clan-full")
                            .replace("%max%", String.valueOf(maxMembers))));
                    return;
                }
                
                clanManager.invitePlayer(target.getUniqueId(), clan.getName());
                player.sendMessage(ColorUtil.translate(
                    plugin.getMessage("prefix") + 
                    " " + plugin.getMessage("invite-sent")
                        .replace("%player%", target.getName())));
                target.sendMessage(ColorUtil.translate(
                    plugin.getMessage("prefix") + 
                    " " + plugin.getMessage("invite-received")
                        .replace("%clan%", clan.getName())));
            });
        }
    }
}
