package com.cubeclans.managers;

import com.cubeclans.database.DatabaseManager;
import com.cubeclans.economy.VaultHook;
import com.cubeclans.models.Clan;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ClanManager {

    private final Plugin plugin;
    private final Map<String, Clan> clans;
    private final Map<UUID, String> playerClans;
    private final Map<UUID, Map<String, Long>> invitations;
    private final DatabaseManager database;
    private final VaultHook vaultHook;
    private final Set<UUID> clanChatEnabled;
    private final Set<UUID> allyChatEnabled;
    
    // Cache control
    private long lastLoadTime = 0;
    private static final long CACHE_TTL_MS = 5000; // 5 seconds cache

    public ClanManager(Plugin plugin, VaultHook vaultHook) {
        this.plugin = plugin;
        this.vaultHook = vaultHook;
        this.clans = new HashMap<>();
        this.playerClans = new HashMap<>();
        this.invitations = new HashMap<>();
        this.database = new DatabaseManager(plugin);
        this.clanChatEnabled = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
        this.allyChatEnabled = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
        
        loadClans();
    }

    public Clan createClan(String name, UUID leader) {
        if (clans.containsKey(name.toLowerCase())) {
            return null; // Clan already exists in cache
        }

        // Verificar costo de creación
        double clanCost = plugin.getConfig().getDouble("settings.clan-creation-cost", 300);

        if (clanCost > 0 && vaultHook.isEnabled()) {
            // Verificar si el jugador tiene suficiente dinero
            String leaderName = Bukkit.getPlayer(leader) != null ? Bukkit.getPlayer(leader).getName() : "Unknown";
            double playerBalance = vaultHook.getEconomy().getBalance(leaderName);

            if (playerBalance < clanCost) {
                // No tiene suficiente dinero, retornar null con mensaje en el comando
                return null;
            }

            // Descontar el dinero
            vaultHook.getEconomy().withdrawPlayer(leaderName, clanCost);
        }

        Clan clan = new Clan(name, leader);
        clans.put(name.toLowerCase(), clan);
        playerClans.put(leader, name.toLowerCase());
        saveClan(clan); // Guardar en BD para sincronizar con otros servidores

        return clan;
    }

    /**
     * Attempt to create a clan and return the result with specific error information
     */
    public ClanCreationResult createClanWithResult(String name, UUID leader) {
        if (clans.containsKey(name.toLowerCase())) {
            return ClanCreationResult.CLAN_EXISTS;
        }

        // Verificar costo de creación
        double clanCost = plugin.getConfig().getDouble("settings.clan-creation-cost", 300);

        if (clanCost > 0 && vaultHook.isEnabled()) {
            // Verificar si el jugador tiene suficiente dinero
            String leaderName = Bukkit.getPlayer(leader) != null ? Bukkit.getPlayer(leader).getName() : "Unknown";
            double playerBalance = vaultHook.getEconomy().getBalance(leaderName);

            if (playerBalance < clanCost) {
                return ClanCreationResult.INSUFFICIENT_FUNDS;
            }

            // Descontar el dinero
            vaultHook.getEconomy().withdrawPlayer(leaderName, clanCost);
        }

        Clan clan = new Clan(name, leader);
        clans.put(name.toLowerCase(), clan);
        playerClans.put(leader, name.toLowerCase());
        saveClan(clan); // Guardar en BD para sincronizar con otros servidores

        return ClanCreationResult.SUCCESS;
    }

    public enum ClanCreationResult {
        SUCCESS,
        CLAN_EXISTS,
        INSUFFICIENT_FUNDS
    }

    public boolean deleteClan(String name) {
        String lowerName = name.toLowerCase();
        Clan clan = clans.remove(lowerName);
        if (clan != null) {
            // Remover todos los miembros del mapping
            for (UUID member : clan.getMembers()) {
                playerClans.remove(member);
                clanChatEnabled.remove(member);
                allyChatEnabled.remove(member);
            }
            // Eliminar de la BD inmediatamente
            database.deleteClan(clan.getName());
            // Forzar resincronización en la siguiente verificación
            lastLoadTime = 0;
            return true;
        }
        return false;
    }

    public Clan getClan(String name) {
        String lowerName = name.toLowerCase();
        Clan cached = clans.get(lowerName);
        
        if (cached != null) {
            // Verificar que el clan aún existe en BD
            Clan fromDB = database.getClanFromDB(name);
            if (fromDB == null) {
                // El clan fue eliminado en otro servidor, remover del cache
                clans.remove(lowerName);
                for (UUID member : cached.getMembers()) {
                    playerClans.remove(member);
                }
                return null;
            }
            return cached; // Existe en BD, devolver cached
        }
        
        // If not in cache, fetch from database
        Clan fromDB = database.getClanFromDB(name);
        if (fromDB != null) {
            clans.put(lowerName, fromDB); // Add to cache
            // Update playerClans mapping for all members
            for (UUID member : fromDB.getMembers()) {
                playerClans.put(member, lowerName);
            }
        }
        return fromDB;
    }

    public Clan getPlayerClan(UUID uuid) {
        // Sincronizar con BD si el caché expiró
        syncClansIfNeeded();
        
        // Buscar el clan del usuario
        String clanName = playerClans.get(uuid);
        return clanName != null ? getClan(clanName) : null;
    }

    public boolean isInClan(UUID uuid) {
        // Sincronizar con BD si el caché expiró
        syncClansIfNeeded();
        
        return playerClans.containsKey(uuid);
    }

    public boolean clanExists(String name) {
        // Verificar en BD para asegurar que no fue eliminado en otro servidor
        Clan fromDB = database.getClanFromDB(name);
        return fromDB != null;
    }

    public List<String> getAllClanNames() {
        // Sincronizar con BD si el caché expiró
        syncClansIfNeeded();
        
        return new ArrayList<>(clans.values().stream()
                .map(Clan::getName)
                .collect(Collectors.toList()));
    }
    
    private void updatePlayerClansMapping() {
        playerClans.clear();
        for (Clan clan : clans.values()) {
            for (UUID member : clan.getMembers()) {
                playerClans.put(member, clan.getName().toLowerCase());
            }
        }
    }

    public void addMemberToClan(Clan clan, UUID uuid) {
        clan.addMember(uuid);
        playerClans.put(uuid, clan.getName().toLowerCase());
        saveClan(clan); // Guardar cambios en BD para sincronizar con otros servidores
    }

    public void removeMemberFromClan(Clan clan, UUID uuid) {
        clan.removeMember(uuid);
        playerClans.remove(uuid);
        clanChatEnabled.remove(uuid);
        allyChatEnabled.remove(uuid);
        saveClan(clan); // Guardar cambios en BD para sincronizar con otros servidores
    }

    public void invitePlayer(UUID player, String clanName) {
        Map<String, Long> playerInvites = invitations.getOrDefault(player, new HashMap<>());
        playerInvites.put(clanName.toLowerCase(), System.currentTimeMillis());
        invitations.put(player, playerInvites);
    }

    public boolean hasInvitation(UUID player, String clanName) {
        Map<String, Long> playerInvites = invitations.get(player);
        if (playerInvites == null) {
            return false;
        }
        
        Long inviteTime = playerInvites.get(clanName.toLowerCase());
        if (inviteTime == null) {
            return false;
        }
        
        // Invitations expire after 5 minutes
        if (System.currentTimeMillis() - inviteTime > 300000) {
            playerInvites.remove(clanName.toLowerCase());
            return false;
        }
        
        return true;
    }

    public void removeInvitation(UUID player, String clanName) {
        Map<String, Long> playerInvites = invitations.get(player);
        if (playerInvites != null) {
            playerInvites.remove(clanName.toLowerCase());
        }
    }

    public Collection<Clan> getAllClans() {
        // Sincronizar con BD si el caché expiró
        syncClansIfNeeded();
        
        // Validar que todos los clanes en cache aún existen en BD
        List<String> toRemove = new ArrayList<>();
        for (Clan clan : clans.values()) {
            if (database.getClanFromDB(clan.getName()) == null) {
                // El clan fue eliminado en otro servidor
                toRemove.add(clan.getName().toLowerCase());
            }
        }
        
        // Remover clanes que ya no existen
        for (String clanName : toRemove) {
            Clan removed = clans.remove(clanName);
            if (removed != null) {
                for (UUID member : removed.getMembers()) {
                    playerClans.remove(member);
                }
            }
        }
        
        return clans.values();
    }

    public int getClanCount() {
        // Sincronizar con BD si el caché expiró
        syncClansIfNeeded();
        
        return clans.size();
    }

    public void saveClan(Clan clan) {
        // Guardar en BD (insert si es nuevo, update si existe)
        database.saveClan(clan);
        // Actualizar en cache también
        clans.put(clan.getName().toLowerCase(), clan);
    }

    public void saveClans() {
        // Guardar todos los clanes actuales sin sincronizar
        // (syncClansIfNeeded se ejecuta cuando se accede a los clanes, no acá)
        for (Clan clan : clans.values()) {
            database.saveClan(clan);
        }
    }

    public void loadClans() {
        // Cargar todos los clanes de BD
        Map<String, Clan> loadedClans = database.loadAllClans();
        
        // Remover clanes que ya no existen en la BD
        clans.clear();
        clans.putAll(loadedClans);
        
        // Reconstruir mapping de jugadores
        playerClans.clear();
        for (Clan clan : clans.values()) {
            for (UUID member : clan.getMembers()) {
                playerClans.put(member, clan.getName().toLowerCase());
            }
        }
        
        lastLoadTime = System.currentTimeMillis();
    }
    
    /**
     * Sync clans from database with proper cache validation
     * Uses retainAll to remove deleted clans instead of putting new ones
     */
    private void syncClansIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLoadTime >= CACHE_TTL_MS) {
            // Cargar clanes actuales desde BD
            Map<String, Clan> dbClans = database.loadAllClans();
            
            // Remover del cache todos los clanes que no existan en BD
            clans.keySet().retainAll(dbClans.keySet());
            
            // Actualizar clanes existentes con datos de BD (en caso de cambios de otros servidores)
            for (Map.Entry<String, Clan> entry : dbClans.entrySet()) {
                clans.put(entry.getKey(), entry.getValue());
            }
            
            // Reconstruir mapping de jugadores a clanes
            playerClans.clear();
            for (Clan clan : clans.values()) {
                for (UUID member : clan.getMembers()) {
                    playerClans.put(member, clan.getName().toLowerCase());
                }
            }
            
            lastLoadTime = System.currentTimeMillis();
        }
    }

    public void close() {
        database.close();
    }

    // Clan chat toggles
    public boolean isClanChatEnabled(UUID uuid) {
        return clanChatEnabled.contains(uuid);
    }

    public void setClanChat(UUID uuid, boolean enabled) {
        if (enabled) {
            clanChatEnabled.add(uuid);
            allyChatEnabled.remove(uuid);
        } else {
            clanChatEnabled.remove(uuid);
        }
    }

    public boolean isAllyChatEnabled(UUID uuid) {
        return allyChatEnabled.contains(uuid);
    }

    public void setAllyChat(UUID uuid, boolean enabled) {
        if (enabled) {
            allyChatEnabled.add(uuid);
            clanChatEnabled.remove(uuid);
        } else {
            allyChatEnabled.remove(uuid);
        }
    }

    // Alliance Management
    public boolean sendAllyRequest(String fromClan, String toClan) {
        Clan from = getClan(fromClan);
        Clan to = getClan(toClan);
        
        if (from == null || to == null) return false;
        if (from.isAlly(to.getName())) return false;
        
        to.addAllyRequest(from.getName());
        saveClan(to); // Guardar cambios
        return true;
    }

    public boolean acceptAllyRequest(String clanName, String requestingClan) {
        Clan clan = getClan(clanName);
        Clan requester = getClan(requestingClan);
        
        if (clan == null || requester == null) return false;
        if (!clan.hasAllyRequest(requester.getName())) return false;
        
        // Remove request and add alliance both ways
        clan.removeAllyRequest(requester.getName());
        clan.addAlly(requester.getName());
        requester.addAlly(clan.getName());
        
        saveClan(clan); // Guardar cambios en ambos clanes
        saveClan(requester);
        return true;
    }

    public boolean denyAllyRequest(String clanName, String requestingClan) {
        Clan clan = getClan(clanName);
        Clan requester = getClan(requestingClan);
        
        if (clan == null || requester == null) return false;
        if (!clan.hasAllyRequest(requester.getName())) return false;
        
        clan.removeAllyRequest(requester.getName());
        saveClan(clan); // Guardar cambios
        return true;
    }

    public boolean removeAlliance(String clan1Name, String clan2Name) {
        Clan clan1 = getClan(clan1Name);
        Clan clan2 = getClan(clan2Name);
        
        if (clan1 == null || clan2 == null) return false;
        if (!clan1.isAlly(clan2.getName())) return false;
        
        clan1.removeAlly(clan2.getName());
        clan2.removeAlly(clan1.getName());
        
        saveClan(clan1); // Guardar cambios en ambos clanes
        saveClan(clan2);
        return true;
    }
}
