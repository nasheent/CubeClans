package com.cubeclans.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Clan {

    private final String name;
    private final UUID leader;
    private final Set<UUID> members;
    private final Map<UUID, Long> joinDates;
    private final Map<UUID, Integer> memberKills; // Kills per member while in clan
    private final Map<UUID, Integer> memberDeaths; // Deaths per member while in clan
    private final Map<UUID, String> memberRanks; // Rank of each member
    private final LocalDateTime creationDate;
    private String description;
    private String colorCode; // e.g., "&f", "&a"
    private final Set<String> allies; // Allied clan names
    private final Set<String> allyRequests; // Pending ally requests received
    private final Set<String> enemies; // Enemy clan names
    private double bankBalance = 0.0;
    private boolean friendlyFireEnabled = false;

    public Clan(String name, UUID leader) {
        this.name = name;
        this.leader = leader;
        this.members = new HashSet<>();
        this.joinDates = new HashMap<>();
        this.memberKills = new HashMap<>();
        this.memberDeaths = new HashMap<>();
        this.memberRanks = new HashMap<>();
        this.creationDate = LocalDateTime.now();
        this.description = "No description set";
        this.colorCode = "&f";
        this.allies = new HashSet<>();
        this.allyRequests = new HashSet<>();
        this.enemies = new HashSet<>();
        
        // Leader is also a member
        this.members.add(leader);
        this.joinDates.put(leader, System.currentTimeMillis());
        this.memberKills.put(leader, 0);
        this.memberDeaths.put(leader, 0);
        this.memberRanks.put(leader, "leader");
    }

    public Clan(String name, UUID leader, Set<UUID> members, Map<UUID, Long> joinDates, 
                LocalDateTime creationDate, String description, Set<String> allies, Set<String> allyRequests,
                Map<UUID, Integer> memberKills, Map<UUID, Integer> memberDeaths, Set<String> enemies) {
        this.name = name;
        this.leader = leader;
        this.members = members;
        this.joinDates = joinDates;
        this.memberKills = memberKills != null ? memberKills : new HashMap<>();
        this.memberDeaths = memberDeaths != null ? memberDeaths : new HashMap<>();
        this.memberRanks = new HashMap<>();
        this.creationDate = creationDate;
        this.description = description;
        this.colorCode = "&f";
        this.allies = allies != null ? allies : new HashSet<>();
        this.allyRequests = allyRequests != null ? allyRequests : new HashSet<>();
        this.enemies = enemies != null ? enemies : new HashSet<>();
        
        // Set default ranks for existing members if not set
        this.memberRanks.put(leader, "leader");
        for (UUID member : members) {
            if (!member.equals(leader) && !this.memberRanks.containsKey(member)) {
                this.memberRanks.put(member, "member");
            }
        }
    }

    public boolean isFriendlyFireEnabled() {
        return friendlyFireEnabled;
    }

    public void setFriendlyFireEnabled(boolean friendlyFireEnabled) {
        this.friendlyFireEnabled = friendlyFireEnabled;
    }

    public String getName() {
        return name;
    }

    public String getColorCode() {
        return colorCode == null ? "&f" : colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getColoredName() {
        return com.cubeclans.utils.ColorUtil.translate(getColorCode() + name);
    }

    public UUID getLeader() {
        return leader;
    }

    public Set<UUID> getMembers() {
        return new HashSet<>(members);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public boolean isLeader(UUID uuid) {
        return leader.equals(uuid);
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
        joinDates.put(uuid, System.currentTimeMillis());
        memberKills.put(uuid, 0);
        memberDeaths.put(uuid, 0);
        memberRanks.put(uuid, "member"); // Default rank
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        joinDates.remove(uuid);
        memberKills.remove(uuid);
        memberDeaths.remove(uuid);
        memberRanks.remove(uuid);
    }

    public int getMemberCount() {
        return members.size();
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public String getFormattedCreationDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return creationDate.format(formatter);
    }

    public long getJoinDate(UUID uuid) {
        return joinDates.getOrDefault(uuid, 0L);
    }

    public String getFormattedJoinDate(UUID uuid) {
        long timestamp = joinDates.getOrDefault(uuid, 0L);
        LocalDateTime date = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp),
            java.time.ZoneId.systemDefault()
        );
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return date.format(formatter);
    }

    // Alliance methods
    public Set<String> getAllies() {
        return new HashSet<>(allies);
    }

    public int getAllyCount() {
        return allies.size();
    }

    public boolean isAlly(String clanName) {
        return allies.contains(clanName);
    }

    public void addAlly(String clanName) {
        allies.add(clanName);
    }

    public void removeAlly(String clanName) {
        allies.remove(clanName);
    }

    public Set<String> getAllyRequests() {
        return new HashSet<>(allyRequests);
    }

    public boolean hasAllyRequest(String clanName) {
        return allyRequests.contains(clanName);
    }

    public void addAllyRequest(String clanName) {
        allyRequests.add(clanName);
    }

    public void removeAllyRequest(String clanName) {
        allyRequests.remove(clanName);
    }

    // Enemy methods
    public Set<String> getEnemies() {
        return new HashSet<>(enemies);
    }

    public boolean isEnemy(String clanName) {
        return enemies.contains(clanName);
    }

    public void addEnemy(String clanName) {
        enemies.add(clanName);
    }

    public void removeEnemy(String clanName) {
        enemies.remove(clanName);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(double amount) {
        this.bankBalance = amount;
    }

    public void depositBank(double amount) {
        this.bankBalance += amount;
    }

    public boolean withdrawBank(double amount) {
        if (bankBalance >= amount) {
            bankBalance -= amount;
            return true;
        }
        return false;
    }

    // KDR methods
    public int getMemberKills(UUID uuid) {
        return memberKills.getOrDefault(uuid, 0);
    }

    public int getMemberDeaths(UUID uuid) {
        return memberDeaths.getOrDefault(uuid, 0);
    }

    public void addMemberKill(UUID uuid) {
        if (members.contains(uuid)) {
            memberKills.put(uuid, memberKills.getOrDefault(uuid, 0) + 1);
        }
    }

    public void addMemberDeath(UUID uuid) {
        if (members.contains(uuid)) {
            memberDeaths.put(uuid, memberDeaths.getOrDefault(uuid, 0) + 1);
        }
    }

    public int getTotalKills() {
        return memberKills.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getTotalDeaths() {
        return memberDeaths.values().stream().mapToInt(Integer::intValue).sum();
    }

    public double getKDR() {
        int deaths = getTotalDeaths();
        if (deaths == 0) {
            return getTotalKills();
        }
        return (double) getTotalKills() / deaths;
    }

    public void resetKDRStats() {
        for (UUID uuid : members) {
            memberKills.put(uuid, 0);
            memberDeaths.put(uuid, 0);
        }
    }

    // Rank methods
    public String getMemberRank(UUID uuid) {
        if (isLeader(uuid)) {
            return "leader";
        }
        return memberRanks.getOrDefault(uuid, "member");
    }

    public void setMemberRank(UUID uuid, String rank) {
        if (!isLeader(uuid)) { // Leaders cannot have their rank changed
            memberRanks.put(uuid, rank);
        }
    }

    public Map<UUID, String> getMemberRanks() {
        return new HashMap<>(memberRanks);
    }

    public boolean hasPermission(UUID uuid, ClanPermission permission, com.cubeclans.CubeClans plugin) {
        if (isLeader(uuid)) {
            return true; // Leader has all permissions
        }
        
        String rankName = getMemberRank(uuid);
        ClanRank rank = plugin.getRank(rankName);
        
        if (rank == null) {
            return false;
        }
        
        return rank.hasPermission(permission);
    }

    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("leader", leader.toString());
        
        List<String> membersList = new ArrayList<>();
        for (UUID member : members) {
            membersList.add(member.toString());
        }
        data.put("members", membersList);
        
        Map<String, Long> joinDatesMap = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : joinDates.entrySet()) {
            joinDatesMap.put(entry.getKey().toString(), entry.getValue());
        }
        data.put("joinDates", joinDatesMap);
        
        // Serialize member ranks
        Map<String, String> ranksMap = new HashMap<>();
        for (Map.Entry<UUID, String> entry : memberRanks.entrySet()) {
            ranksMap.put(entry.getKey().toString(), entry.getValue());
        }
        data.put("memberRanks", ranksMap);
        
        data.put("creationDate", creationDate.toString());
        data.put("description", description);
        data.put("color", getColorCode());
        data.put("allies", new ArrayList<>(allies));
        data.put("allyRequests", new ArrayList<>(allyRequests));
        data.put("enemies", new ArrayList<>(enemies));
        data.put("bankBalance", bankBalance);
        data.put("friendlyFireEnabled", friendlyFireEnabled);
        
        return data;
    }

    public static Clan deserialize(Map<String, Object> data) {
        String name = (String) data.get("name");
        UUID leader = UUID.fromString((String) data.get("leader"));
        
        Set<UUID> members = new HashSet<>();
        @SuppressWarnings("unchecked")
        List<String> membersList = (List<String>) data.get("members");
        for (String memberStr : membersList) {
            members.add(UUID.fromString(memberStr));
        }
        
        Map<UUID, Long> joinDates = new HashMap<>();
        @SuppressWarnings("unchecked")
        Map<String, Object> joinDatesMap = (Map<String, Object>) data.get("joinDates");
        for (Map.Entry<String, Object> entry : joinDatesMap.entrySet()) {
            UUID uuid = UUID.fromString(entry.getKey());
            Long timestamp = entry.getValue() instanceof Integer ? 
                ((Integer) entry.getValue()).longValue() : (Long) entry.getValue();
            joinDates.put(uuid, timestamp);
        }
        
        LocalDateTime creationDate = LocalDateTime.parse((String) data.get("creationDate"));
        String description = (String) data.getOrDefault("description", "No description set");
        
        Set<String> allies = new HashSet<>();
        if (data.containsKey("allies")) {
            @SuppressWarnings("unchecked")
            List<String> alliesList = (List<String>) data.get("allies");
            allies.addAll(alliesList);
        }
        
        Set<String> allyRequests = new HashSet<>();
        if (data.containsKey("allyRequests")) {
            @SuppressWarnings("unchecked")
            List<String> requestsList = (List<String>) data.get("allyRequests");
            allyRequests.addAll(requestsList);
        }
        
        Map<UUID, Integer> memberKills = new HashMap<>();
        Map<UUID, Integer> memberDeaths = new HashMap<>();
        // Initialize kills/deaths to 0 for all members if not present
        for (UUID member : members) {
            memberKills.put(member, 0);
            memberDeaths.put(member, 0);
        }
        
        double bankBalance = 0.0;
        if (data.containsKey("bankBalance")) {
            Object bankObj = data.get("bankBalance");
            if (bankObj instanceof Number) {
                bankBalance = ((Number) bankObj).doubleValue();
            } else if (bankObj instanceof String) {
                try {
                    bankBalance = Double.parseDouble((String) bankObj);
                } catch (NumberFormatException ignored) {}
            }
        }
        Set<String> enemies = new HashSet<>();
        if (data.containsKey("enemies")) {
            @SuppressWarnings("unchecked")
            List<String> enemiesList = (List<String>) data.get("enemies");
            if (enemiesList != null) {
                enemies.addAll(enemiesList);
            }
        }
        
        Clan clan = new Clan(name, leader, members, joinDates, creationDate, description, allies, allyRequests, memberKills, memberDeaths, enemies);
        
        // Load member ranks if present
        if (data.containsKey("memberRanks")) {
            @SuppressWarnings("unchecked")
            Map<String, String> ranksMap = (Map<String, String>) data.get("memberRanks");
            if (ranksMap != null) {
                for (Map.Entry<String, String> entry : ranksMap.entrySet()) {
                    UUID uuid = UUID.fromString(entry.getKey());
                    clan.memberRanks.put(uuid, entry.getValue());
                }
            }
        }
        
        clan.setBankBalance(bankBalance);
        Object colorObj = data.get("color");
        if (colorObj instanceof String) {
            clan.setColorCode((String) colorObj);
        }
        Object ffObj = data.get("friendlyFireEnabled");
        if (ffObj instanceof Boolean) {
            clan.setFriendlyFireEnabled((Boolean) ffObj);
        }
        return clan;
    }
}
