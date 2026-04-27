package com.cubeclans.menus;

import com.cubeclans.CubeClans;
import com.cubeclans.models.Clan;
import com.cubeclans.utils.ColorUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import com.cubeclans.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ClanMenu {

    private final CubeClans plugin;
    private final Player player;

    public ClanMenu(CubeClans plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openMainMenu() {
        ConfigurationSection menuConfig = plugin.getConfig().getConfigurationSection("menu.main");
        if (menuConfig == null) {
            player.sendMessage(ColorUtil.translate("&cMenu configuration error!"));
            return;
        }

        String title = ColorUtil.translate(menuConfig.getString("title", "&8Clan Menu"));
        int size = menuConfig.getInt("size", 27);
        Inventory inv = Bukkit.createInventory(null, size, title);

        boolean inClan = plugin.getClanManager().isInClan(player.getUniqueId());
        Clan playerClan = null;
        boolean isLeader = false;
        if (inClan) {
            playerClan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (playerClan != null) {
                isLeader = playerClan.isLeader(player.getUniqueId());
            }
        }

        ConfigurationSection items = menuConfig.getConfigurationSection("items");
        if (items != null) {
            // Fill background
            if (items.contains("fill")) {
                ConfigurationSection fillConfig = items.getConfigurationSection("fill");
                String matName = fillConfig.getString("material", "GRAY_STAINED_GLASS_PANE");
                ItemStack fillItem = new ItemBuilder(matName).name(fillConfig.getString("name", " ")).lore(fillConfig.getStringList("lore")).build();
                List<Integer> slots = fillConfig.getIntegerList("slots");
                for (int slot : slots) {
                    inv.setItem(slot, fillItem);
                }
            }

            // Add menu items
            if (inClan) {
                // Show details button instead of separate info/members/color
                addMenuItem(inv, items, "clan-details", player);
                // Player stats item (personal stats inside clan)
                if (items.contains("player-stats") && playerClan != null) {
                    ConfigurationSection statsCfg = items.getConfigurationSection("player-stats");
                    int statSlot = statsCfg.getInt("slot", 24);
                    try {
                        Material mat = Material.valueOf(statsCfg.getString("material", "PAPER"));
                        int kills = playerClan.getMemberKills(player.getUniqueId());
                        int deaths = playerClan.getMemberDeaths(player.getUniqueId());
                        double kdr = deaths == 0 ? kills : (double) kills / deaths;
                    String name = ColorUtil.translate(statsCfg.getString("name", "&eYour Stats"))
                                .replace("%clan_player_kills%", String.valueOf(kills))
                                .replace("%clan_player_deaths%", String.valueOf(deaths))
                        .replace("%clan_player_kdr%", String.format("%.2f", kdr))
                        .replace("%clan_player_kill_percent%", totalKillsPercent(playerClan, player));
                        List<String> lore = new ArrayList<>();
                        for (String line : statsCfg.getStringList("lore")) {
                                String working = line
                                    .replace("%clan_player_kills%", String.valueOf(kills))
                                    .replace("%clan_player_deaths%", String.valueOf(deaths))
                                    .replace("%clan_player_kdr%", String.format("%.2f", kdr))
                                    .replace("%clan_player_kill_percent%", totalKillsPercent(playerClan, player));
                            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                                working = PlaceholderAPI.setPlaceholders(player, working);
                            }
                            lore.add(ColorUtil.translate(working));
                        }
                        ItemStack statsItem = new ItemBuilder(mat).name(name).lore(lore).build();
                        inv.setItem(statSlot, statsItem);
                    } catch (Exception ignored) {}
                }
                if (isLeader) {
                    addMenuItem(inv, items, "invite-player", player);
                }
                addMenuItem(inv, items, "clan-list", player);
            } else {
                addMenuItem(inv, items, "create-clan", player);
            }
        }

        player.openInventory(inv);
    }

    private String totalKillsPercent(Clan clan, Player player) {
        try {
            int playerKills = clan.getMemberKills(player.getUniqueId());
            int totalKills = clan.getTotalKills();
            if (totalKills <= 0) return "0.00";
            double percent = (playerKills / (double) totalKills) * 100.0;
            return String.format("%.2f", percent);
        } catch (Exception e) {
            return "0.00";
        }
    }

    public void openClanDetailsMenu() {
        ConfigurationSection menuConfig = plugin.getConfig().getConfigurationSection("menu.clan-details");
        if (menuConfig == null) {
            player.sendMessage(ColorUtil.translate("&cMenu configuration error!"));
            return;
        }

        boolean inClan = plugin.getClanManager().isInClan(player.getUniqueId());
        if (!inClan) {
            player.sendMessage(ColorUtil.translate("&cYou are not in a clan."));
            return;
        }
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(ColorUtil.translate("&cClan not found."));
            return;
        }

        String title = ColorUtil.translate(menuConfig.getString("title", "&8Clan Details"));
        int size = menuConfig.getInt("size", 45);
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Top-level fill (same pattern as color / members / list)
        if (menuConfig.isConfigurationSection("fill")) {
            ConfigurationSection fillCfg = menuConfig.getConfigurationSection("fill");
            try {
                String matName = fillCfg.getString("material", "GRAY_STAINED_GLASS_PANE");
                ItemStack fillItem = new ItemBuilder(matName)
                        .name(fillCfg.getString("name", " "))
                        .lore(fillCfg.getStringList("lore"))
                        .build();
                for (int s : fillCfg.getIntegerList("slots")) inv.setItem(s, fillItem);
            } catch (Exception ignored) {}
        }

        ConfigurationSection items = menuConfig.getConfigurationSection("items");
        if (items != null) {
            // Fill inside items (like main menu style) if present
            if (items.isConfigurationSection("fill")) {
                ConfigurationSection fillCfg = items.getConfigurationSection("fill");
                try {
                    String matName = fillCfg.getString("material", "GRAY_STAINED_GLASS_PANE");
                    ItemStack fillItem = new ItemBuilder(matName)
                            .name(fillCfg.getString("name", " "))
                            .lore(fillCfg.getStringList("lore"))
                            .build();
                    for (int s : fillCfg.getIntegerList("slots")) inv.setItem(s, fillItem);
                } catch (Exception ignored) {}
            }
            // Info item
            if (items.contains("info")) {
                ConfigurationSection infoCfg = items.getConfigurationSection("info");
                int slot = infoCfg.getInt("slot", 20);
                String materialName = infoCfg.getString("material", "PLAYER_HEAD");
                String name = ColorUtil.translate(infoCfg.getString("name", "&bClan Information"));
                List<String> lore = new ArrayList<>();
                for (String line : infoCfg.getStringList("lore")) {
                    String working = line;
                    if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                        working = PlaceholderAPI.setPlaceholders(player, working);
                    }
                    working = working
                            .replace("%leader%", Bukkit.getOfflinePlayer(clan.getLeader()).getName())
                            .replace("%members%", String.valueOf(clan.getMemberCount()))
                            .replace("%created%", clan.getFormattedCreationDate())
                            .replace("%cubeclans_bank%", String.format("%.2f", clan.getBankBalance()))
                            .replace("%cubeclans_kdr%", String.format("%.2f", clan.getKDR()))
                            .replace("%cubeclans_kills%", String.valueOf(clan.getTotalKills()))
                            .replace("%cubeclans_deaths%", String.valueOf(clan.getTotalDeaths()));
                    lore.add(ColorUtil.translate(working));
                }
                ItemStack infoItem = new ItemBuilder(materialName).name(name).lore(lore).build();
                // If skull, set owner appropriately per version
                try {
                    org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) infoItem.getItemMeta();
                    if (skullMeta != null) {
                        if (com.cubeclans.utils.VersionHelper.is1_8()) {
                            skullMeta.setOwner(Bukkit.getOfflinePlayer(clan.getLeader()).getName());
                        } else {
                            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(clan.getLeader()));
                        }
                        infoItem.setItemMeta(skullMeta);
                    }
                } catch (Exception ignored) {}
                inv.setItem(slot, infoItem);
            }

            // Members item
            if (items.contains("members")) {
                addMenuItem(inv, items, "members", player);
            }

            // Color item
            if (items.contains("color")) {
                addMenuItem(inv, items, "color", player);
            }

            // Relations item (allies & enemies info)
            if (items.contains("relations")) {
                addMenuItem(inv, items, "relations", player);
            }

            // Back item
            if (items.contains("back")) {
                addMenuItem(inv, items, "back", player);
            }
        }

        player.openInventory(inv);
    }

    public void openMembersMenu(Clan clan) {
        openMembersMenu(clan, 0);
    }

    public void openMembersMenu(Clan clan, int page) {
        ConfigurationSection menuConfig = plugin.getConfig().getConfigurationSection("menu.members");
        if (menuConfig == null) {
            player.sendMessage(ColorUtil.translate("&cMenu configuration error!"));
            return;
        }

        String membersTitleRaw = menuConfig.getString("title", "&8Clan Members");
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            membersTitleRaw = PlaceholderAPI.setPlaceholders(player, membersTitleRaw);
        }
        membersTitleRaw = membersTitleRaw.replace("%clan%", clan.getColoredName());
        String title = ColorUtil.translate(membersTitleRaw);
        int size = menuConfig.getInt("size", 54);
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Fill
        if (menuConfig.isConfigurationSection("fill")) {
            ConfigurationSection fillCfg = menuConfig.getConfigurationSection("fill");
            try {
                String matName = fillCfg.getString("material", "GRAY_STAINED_GLASS_PANE");
                ItemStack fillItem = new ItemBuilder(matName).name(fillCfg.getString("name", " ")).lore(fillCfg.getStringList("lore")).build();
                for (int s : fillCfg.getIntegerList("slots")) inv.setItem(s, fillItem);
            } catch (Exception ignored) {}
        }

        int[] memberSlots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};
        List<java.util.UUID> members = new ArrayList<>(clan.getMembers());
        // Ordenar por fecha de ingreso ascendente (más antiguos primero)
        members.sort(java.util.Comparator.comparingLong(clan::getJoinDate));
        int perPage = memberSlots.length;
        int maxPage = (int) Math.ceil(members.size() / (double) perPage) - 1;
        if (maxPage < 0) maxPage = 0;
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;
        int startIndex = page * perPage;

        for (int i = 0; i < perPage; i++) {
            int idx = startIndex + i;
            if (idx >= members.size()) break;
            java.util.UUID memberUUID = members.get(idx);
            boolean isLeader = clan.isLeader(memberUUID);
            ConfigurationSection itemConfig = isLeader ? menuConfig.getConfigurationSection("leader-item") : menuConfig.getConfigurationSection("member-item");
            if (itemConfig == null) continue;
            try {
                Material material = Material.valueOf(itemConfig.getString("material", "PLAYER_HEAD"));
                String playerName = Bukkit.getOfflinePlayer(memberUUID).getName();
                String nameRaw = itemConfig.getString("name", "&a%player%").replace("%player%", playerName);
                String name = ColorUtil.translate(nameRaw);
                List<String> lore = new ArrayList<>();
                for (String line : itemConfig.getStringList("lore")) {
                    String working = line;
                    if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) working = PlaceholderAPI.setPlaceholders(player, working);
                    working = working.replace("%player%", playerName).replace("%joined%", clan.getFormattedJoinDate(memberUUID)).replace("%role%", isLeader ? "Leader" : "Member");
                    lore.add(ColorUtil.translate(working));
                }
                ItemStack head = new ItemBuilder(material).name(name).lore(lore).build();
                if (material == Material.PLAYER_HEAD) {
                    try {
                        org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
                        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(memberUUID));
                        head.setItemMeta(skullMeta);
                    } catch (Exception ignored) {}
                }
                int targetSlot = memberSlots[i];
                if (targetSlot >= 0 && targetSlot < size) inv.setItem(targetSlot, head);
            } catch (Exception ignored) {}
        }

        // Back
        ConfigurationSection backConfig = menuConfig.getConfigurationSection("back");
        if (backConfig != null) {
            try {
                int backSlot = backConfig.getInt("slot", 49);
                Material mat = Material.valueOf(backConfig.getString("material", "ARROW"));
                ItemStack backItem = new ItemBuilder(mat)
                        .name(ColorUtil.translate(backConfig.getString("name", "&cBack")))
                        .lore(ColorUtil.translate(backConfig.getStringList("lore")))
                        .menuItemType("back")
                        .build();
                inv.setItem(backSlot, backItem);
            } catch (Exception ignored) {}
        }

        // Previous page
        if (page > 0 && menuConfig.isConfigurationSection("previous-page")) {
            ConfigurationSection prev = menuConfig.getConfigurationSection("previous-page");
            try {
                int prevSlot = prev.getInt("slot", 45);
                Material mat = Material.valueOf(prev.getString("material", "ARROW"));
                ItemStack prevItem = new ItemBuilder(mat)
                        .name(ColorUtil.translate(prev.getString("name", "&ePrevious Page")))
                        .lore(ColorUtil.translate(prev.getStringList("lore")))
                        .menuItemType("previous-page")
                        .build();
                inv.setItem(prevSlot, prevItem);
            } catch (Exception ignored) {}
        }

        // Next page
        if (page < maxPage && menuConfig.isConfigurationSection("next-page")) {
            ConfigurationSection next = menuConfig.getConfigurationSection("next-page");
            try {
                int nextSlot = next.getInt("slot", 53);
                Material mat = Material.valueOf(next.getString("material", "ARROW"));
                ItemStack nextItem = new ItemBuilder(mat)
                        .name(ColorUtil.translate(next.getString("name", "&eNext Page")))
                        .lore(ColorUtil.translate(next.getStringList("lore")))
                        .menuItemType("next-page")
                        .build();
                inv.setItem(nextSlot, nextItem);
            } catch (Exception ignored) {}
        }

        player.openInventory(inv);
    }

    private void addMenuItem(Inventory inv, ConfigurationSection items, String itemKey, Player player) {
        if (!items.contains(itemKey)) return;
        
        ConfigurationSection itemConfig = items.getConfigurationSection(itemKey);
        if (itemConfig == null) return;
        
        int slot = itemConfig.getInt("slot", 0);
        ItemStack item = createMenuItem(itemConfig, player);
        
        // Mark item with its type for identification independent of slot
        ItemBuilder builder = new ItemBuilder(item);
        builder.menuItemType(itemKey);
        ItemStack markedItem = builder.build();
        
        inv.setItem(slot, markedItem);
    }

    private ItemStack createMenuItem(ConfigurationSection config, Player player) {
        String materialName = config.getString("material", "STONE");
        String name = config.getString("name", "");
        List<String> loreRaw = config.getStringList("lore");
        
        // Apply PlaceholderAPI to name
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            name = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, name);
        }
        name = ColorUtil.translate(name);
        
        // Apply PlaceholderAPI to lore
        List<String> lore = new ArrayList<>();
        for (String line : loreRaw) {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                line = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, line);
            }
            lore.add(ColorUtil.translate(line));
        }
        
        ItemBuilder builder = new ItemBuilder(materialName)
                .name(name)
                .lore(lore);
        
        // Apply texture if provided for PLAYER_HEAD
        if (config.contains("texture")) {
            String texture = config.getString("texture");
            builder.texture(texture);
        }
        
        return builder.build();
    }

    public void openColorMenu(Clan clan) {
        String title = ColorUtil.translate(plugin.getConfig().getString("menu.color-selector.title", "&8Clan Color"));
        int size = plugin.getConfig().getInt("menu.color-selector.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Fill background if configured
        if (plugin.getConfig().isConfigurationSection("menu.color-selector.fill")) {
            org.bukkit.configuration.ConfigurationSection fill = plugin.getConfig().getConfigurationSection("menu.color-selector.fill");
            String matName = fill.getString("material", "GRAY_STAINED_GLASS_PANE");
            ItemStack fillItem = new ItemBuilder(matName).name(fill.getString("name", " ")).lore(fill.getStringList("lore")).build();
            if (fill.isList("slots")) {
                for (int s : fill.getIntegerList("slots")) inv.setItem(s, fillItem);
            } else {
                for (int i = 0; i < size; i++) inv.setItem(i, fillItem);
            }
        }

        // Build from config list (supporting 1.8 legacy materials via VersionHelper)
        java.util.List<java.util.Map<?, ?>> colors = plugin.getConfig().getMapList("menu.color-selector.colors");
        for (java.util.Map<?, ?> rawEntry : colors) {
            java.util.Map<String, Object> entry = new java.util.HashMap<>();
            for (java.util.Map.Entry<?, ?> e : rawEntry.entrySet()) entry.put(String.valueOf(e.getKey()), e.getValue());
            int slot = (int) entry.getOrDefault("slot", -1);
            String materialName = String.valueOf(entry.getOrDefault("material", "WHITE_WOOL"));
            String name = String.valueOf(entry.getOrDefault("name", "&fColor"));
            java.util.List<String> lore = java.util.Collections.emptyList();
            Object loreObj = entry.get("lore");
            if (loreObj instanceof java.util.List) {
                java.util.List<?> l = (java.util.List<?>) loreObj;
                java.util.List<String> tmp = new java.util.ArrayList<>();
                for (Object o : l) tmp.add(String.valueOf(o));
                lore = tmp;
            }
            ItemStack colorItem = new ItemBuilder(materialName).name(name).lore(lore.isEmpty() ? java.util.Collections.emptyList() : lore).build();
            if (slot >= 0 && slot < inv.getSize()) inv.setItem(slot, colorItem);
        }

        // Back button
        org.bukkit.configuration.ConfigurationSection back = plugin.getConfig().getConfigurationSection("menu.color-selector.back");
        if (back != null) {
            int backSlot = back.getInt("slot", size - 5);
            Material mat = Material.valueOf(back.getString("material", "ARROW"));
            ItemStack backItem = new ItemBuilder(mat)
                    .name(back.getString("name", "&cBack"))
                    .lore(back.getStringList("lore"))
                    .build();
            inv.setItem(backSlot, backItem);
        }

        player.openInventory(inv);
    }

    private void addColorItem(Inventory inv, int slot, Material mat, String name, java.util.List<String> lore) {
        // Deprecated path kept for compatibility; not used now.
        ItemStack item = new ItemBuilder(mat).name(name).lore(lore).build();
        if (slot >= 0 && slot < inv.getSize()) inv.setItem(slot, item);
    }

    public void openClanListMenu() {
        openClanListMenu(0);
    }

    public void openClanListMenu(int page) {
        ConfigurationSection menuConfig = plugin.getConfig().getConfigurationSection("menu.clan-list");
        if (menuConfig == null) {
            player.sendMessage(ColorUtil.translate("&cMenu configuration error!"));
            return;
        }

        String title = ColorUtil.translate(menuConfig.getString("title", "&8Clan list"));
        int size = menuConfig.getInt("size", 54);
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Fill background if configured
        if (menuConfig.isConfigurationSection("fill")) {
            ConfigurationSection fillCfg = menuConfig.getConfigurationSection("fill");
            try {
                String matName = fillCfg.getString("material", "GRAY_STAINED_GLASS_PANE");
                ItemStack fillItem = new ItemBuilder(matName)
                        .name(fillCfg.getString("name", " "))
                        .lore(fillCfg.getStringList("lore"))
                        .build();
                for (int s : fillCfg.getIntegerList("slots")) inv.setItem(s, fillItem);
            } catch (Exception ignored) {}
        }

        // Get all clans and sort by KDR
        List<Clan> allClans = new ArrayList<>(plugin.getClanManager().getAllClans());
        
        // Sort by KDR descending, then by total kills if tied
        allClans.sort((c1, c2) -> {
            double kdr1 = c1.getKDR();
            double kdr2 = c2.getKDR();
            
            if (Math.abs(kdr1 - kdr2) < 0.01) { // If KDR is essentially equal
                // Sort by who reached this KDR first (higher kills = reached it earlier in most cases)
                return Integer.compare(c2.getTotalKills(), c1.getTotalKills());
            }
            return Double.compare(kdr2, kdr1);
        });

        int[] slotsLayout = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};
        int perPage = slotsLayout.length;
        int maxPage = (int) Math.ceil(allClans.size() / (double) perPage) - 1;
        if (maxPage < 0) maxPage = 0;
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        if (allClans.isEmpty()) {
            // Show "no clans" message
            ConfigurationSection noClansCfg = menuConfig.getConfigurationSection("no-clans");
            if (noClansCfg != null) {
                int slot = noClansCfg.getInt("slot", 22);
                Material mat = Material.valueOf(noClansCfg.getString("material", "BARRIER"));
                String name = ColorUtil.translate(noClansCfg.getString("name", "&cNo Clans"));
                List<String> lore = ColorUtil.translate(noClansCfg.getStringList("lore"));
                
                ItemStack item = new ItemBuilder(mat).name(name).lore(lore).build();
                inv.setItem(slot, item);
            }
        } else {
            ConfigurationSection clanItemCfg = menuConfig.getConfigurationSection("clan-item");
            if (clanItemCfg != null) {
                int startIndex = page * perPage;
                int rank = startIndex + 1;
                for (int i = 0; i < perPage; i++) {
                    int clanIndex = startIndex + i;
                    if (clanIndex >= allClans.size()) break;
                    Clan clan = allClans.get(clanIndex);
                    String matName = clanItemCfg.getString("material", "WHITE_BANNER");
                    Material mat;
                    try { mat = Material.valueOf(matName); } catch (IllegalArgumentException ex) { mat = Material.PAPER; }
                    String nameTemplate = clanItemCfg.getString("name", "&e#{rank} {clan_name}");
                    String leaderName = Bukkit.getOfflinePlayer(clan.getLeader()).getName();
                    String name = ColorUtil.translate(nameTemplate
                            .replace("{rank}", String.valueOf(rank))
                            .replace("{clan_name}", clan.getColoredName()));
                    List<String> lore = new ArrayList<>();
                    for (String line : clanItemCfg.getStringList("lore")) {
                        String loreLine = line
                                .replace("{rank}", String.valueOf(rank))
                                .replace("{clan_name}", clan.getName())
                                .replace("{leader}", leaderName)
                                .replace("{members}", String.valueOf(clan.getMemberCount()))
                                .replace("{kdr}", String.format("%.2f", clan.getKDR()))
                                .replace("{kills}", String.valueOf(clan.getTotalKills()))
                                .replace("{deaths}", String.valueOf(clan.getTotalDeaths()));
                        lore.add(ColorUtil.translate(loreLine));
                    }
                    int targetSlot = slotsLayout[i];
                    if (targetSlot >= 0 && targetSlot < size) {
                        inv.setItem(targetSlot, new ItemBuilder(mat).name(name).lore(lore).build());
                    }
                    rank++;
                }
            }
        }

        // Back button
        ConfigurationSection backCfg = menuConfig.getConfigurationSection("back");
        if (backCfg != null) {
            int backSlot = backCfg.getInt("slot", 49);
            try {
                Material mat = Material.valueOf(backCfg.getString("material", "ARROW"));
                ItemStack item = new ItemBuilder(mat)
                        .name(ColorUtil.translate(backCfg.getString("name", "&cClose")))
                        .lore(ColorUtil.translate(backCfg.getStringList("lore")))
                        .menuItemType("back")
                        .build();
                inv.setItem(backSlot, item);
            } catch (Exception ignored) {}
        }

        // Previous page
        if (page > 0 && menuConfig.isConfigurationSection("previous-page")) {
            ConfigurationSection prev = menuConfig.getConfigurationSection("previous-page");
            try {
                int prevSlot = prev.getInt("slot", 45);
                Material mat = Material.valueOf(prev.getString("material", "ARROW"));
                ItemStack prevItem = new ItemBuilder(mat)
                    .name(ColorUtil.translate(prev.getString("name", "&ePrevious Page")))
                    .lore(ColorUtil.translate(prev.getStringList("lore")))
                    .menuItemType("previous-page")
                    .build();
                inv.setItem(prevSlot, prevItem);
            } catch (Exception ignored) {}
        }

        // Next page
        if (page < maxPage && menuConfig.isConfigurationSection("next-page")) {
            ConfigurationSection next = menuConfig.getConfigurationSection("next-page");
            try {
                int nextSlot = next.getInt("slot", 53);
                Material mat = Material.valueOf(next.getString("material", "ARROW"));
                ItemStack nextItem = new ItemBuilder(mat)
                    .name(ColorUtil.translate(next.getString("name", "&eNext Page")))
                    .lore(ColorUtil.translate(next.getStringList("lore")))
                    .menuItemType("next-page")
                    .build();
                inv.setItem(nextSlot, nextItem);
            } catch (Exception ignored) {}
        }

        player.openInventory(inv);
    }
}
