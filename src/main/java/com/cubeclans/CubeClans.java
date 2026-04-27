package com.cubeclans;

import com.cubeclans.api.CubeClansAPI;
import com.cubeclans.commands.ClanCommand;
import com.cubeclans.listeners.ClanProtectionListener;
import com.cubeclans.listeners.PlayerDeathListener;
import com.cubeclans.listeners.MenuListener;
import com.cubeclans.listeners.ClanChatListener;
import com.cubeclans.managers.ClanManager;
import com.cubeclans.models.ClanRank;
import com.cubeclans.placeholders.ClanPlaceholders;
import com.cubeclans.placeholders.CubePlayerPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CubeClans extends JavaPlugin {

    private ClanManager clanManager;
    private com.cubeclans.economy.VaultHook vaultHook;
    private FileConfiguration messagesConfig;
    private FileConfiguration ranksConfig;
    private Map<String, ClanRank> ranks;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        loadMessages();
        loadRanks();
        
        // Initialize Vault hook first
        vaultHook = new com.cubeclans.economy.VaultHook(this);
        
        // Initialize managers
        clanManager = new ClanManager(this, vaultHook);
        CubeClansAPI.init(this);
        
        // Register commands
        ClanCommand clanCommand = new ClanCommand(this);
        getCommand("clan").setExecutor(clanCommand);
        getCommand("clan").setTabCompleter(clanCommand);
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new ClanProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ClanChatListener(this), this);
        
        // Register PlaceholderAPI expansion
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ClanPlaceholders(this).register();
            new CubePlayerPlaceholders(this).register();
            getLogger().info("PlaceholderAPI expansion registered successfully!");
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
        }
        
        // Auto-save task
        int autoSaveInterval = getConfig().getInt("settings.auto-save-interval", 6000);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            clanManager.saveClans();
        }, autoSaveInterval, autoSaveInterval);
        
        getLogger().info("CubeClans has been enabled!");
        getLogger().info("Loaded " + clanManager.getClanCount() + " clans.");
    }

    @Override
    public void onDisable() {
        // Save all clan data and close database
        if (clanManager != null) {
            clanManager.saveClans();
            clanManager.close();
        }
        
        getLogger().info("CubeClans has been disabled!");
    }

    public ClanManager getClanManager() {
        return clanManager;
    }
    
    private void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    private void loadRanks() {
        File ranksFile = new File(getDataFolder(), "ranks.yml");
        if (!ranksFile.exists()) {
            saveResource("ranks.yml", false);
        }
        ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);
        ranks = new HashMap<>();
        
        ConfigurationSection ranksSection = ranksConfig.getConfigurationSection("ranks");
        if (ranksSection != null) {
            for (String rankName : ranksSection.getKeys(false)) {
                ConfigurationSection rankSection = ranksSection.getConfigurationSection(rankName);
                if (rankSection != null) {
                    String displayName = rankSection.getString("display-name", rankName);
                    String prefix = rankSection.getString("prefix", "");
                    int ladderPosition = rankSection.getInt("ladder-position", 0);
                    boolean isDefault = rankSection.getBoolean("default", false);
                    List<String> permissions = rankSection.getStringList("permissions");
                    
                    ClanRank rank = new ClanRank(rankName, displayName, prefix, ladderPosition, isDefault, permissions);
                    ranks.put(rankName.toLowerCase(), rank);
                }
            }
        }
        
        getLogger().info("Loaded " + ranks.size() + " clan ranks.");
    }
    
    public String getMessage(String key) {
        return messagesConfig.getString(key, "&cMessage not found: " + key);
    }
    
    public void reloadMessages() {
        loadMessages();
    }
    
    public void reloadRanks() {
        loadRanks();
    }
    
    public ClanRank getRank(String rankName) {
        return ranks.get(rankName.toLowerCase());
    }
    
    public ClanRank getDefaultRank() {
        for (ClanRank rank : ranks.values()) {
            if (rank.isDefault()) {
                return rank;
            }
        }
        // Fallback to member rank if no default is set
        return ranks.get("member");
    }
    
    public Map<String, ClanRank> getRanks() {
        return ranks;
    }


    
    public com.cubeclans.economy.VaultHook getVaultHook() {
        return vaultHook;
    }
}
