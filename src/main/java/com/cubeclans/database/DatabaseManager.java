package com.cubeclans.database;

import com.cubeclans.models.Clan;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

public class DatabaseManager {

    private final Plugin plugin;
    private final Logger logger;
    private Connection connection;
    private HikariDataSource dataSource;
    private final String databaseType;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.databaseType = plugin.getConfig().getString("database.type", "SQLITE").toUpperCase();
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            if ("MYSQL".equals(databaseType)) {
                initializeMySQL();
            } else {
                initializeSQLite();
            }

            // Create tables
            createTables();

            logger.info(databaseType + " database initialized successfully.");
        } catch (SQLException e) {
            logger.severe("Could not initialize " + databaseType + " database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeSQLite() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        String url = "jdbc:sqlite:" + dataFolder.getAbsolutePath() + File.separator + "clans.db";
        connection = DriverManager.getConnection(url);
    }

    private void initializeMySQL() {
        HikariConfig config = new HikariConfig();
        
        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("database.mysql.port", 3306);
        String database = plugin.getConfig().getString("database.mysql.database", "cubeclans");
        String username = plugin.getConfig().getString("database.mysql.username", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "password");
        
        config.setJdbcUrl("jdbc:mysql://" + host +":" + port + "/" + database + "?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true");
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // Pool settings from config
        config.setMaximumPoolSize(plugin.getConfig().getInt("database.mysql.pool.maximum-pool-size", 10));
        config.setMinimumIdle(plugin.getConfig().getInt("database.mysql.pool.minimum-idle", 2));
        config.setConnectionTimeout(plugin.getConfig().getLong("database.mysql.pool.connection-timeout", 5000));
        config.setIdleTimeout(plugin.getConfig().getLong("database.mysql.pool.idle-timeout", 600000));
        config.setMaxLifetime(plugin.getConfig().getLong("database.mysql.pool.max-lifetime", 1800000));
        
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        dataSource = new HikariDataSource(config);
    }

    private void createTables() throws SQLException {
        boolean isMySQL = "MYSQL".equals(databaseType);
        
        String textType = isMySQL ? "VARCHAR(255)" : "TEXT";
        String longTextType = isMySQL ? "TEXT" : "TEXT";
        String realType = isMySQL ? "DOUBLE" : "REAL";
        String autoIncrement = isMySQL ? "AUTO_INCREMENT" : "AUTOINCREMENT";
        String bigIntType = isMySQL ? "BIGINT" : "INTEGER";
        String booleanType = isMySQL ? "TINYINT(1)" : "INTEGER";
        
        String clansTable = "CREATE TABLE IF NOT EXISTS clans (" +
                "name " + textType + " PRIMARY KEY," +
                "leader " + textType + " NOT NULL," +
                "creation_date " + textType + " NOT NULL," +
                "description " + longTextType + "," +
                "color_code " + textType + "," +
            "bank_balance " + realType + " DEFAULT 0," +
            "friendly_fire " + booleanType + " DEFAULT 0" +
                (isMySQL ? ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;" : ");");

        String membersTable = "CREATE TABLE IF NOT EXISTS clan_members (" +
                "id INTEGER PRIMARY KEY " + autoIncrement + "," +
                "clan_name " + textType + " NOT NULL," +
                "member_uuid " + textType + " NOT NULL," +
                "join_date " + bigIntType + " NOT NULL," +
                "kills INTEGER DEFAULT 0," +
                "deaths INTEGER DEFAULT 0," +
                (isMySQL ? "FOREIGN KEY (clan_name) REFERENCES clans(name) ON DELETE CASCADE," : "FOREIGN KEY (clan_name) REFERENCES clans(name) ON DELETE CASCADE,") +
                "UNIQUE(clan_name, member_uuid)" +
                (isMySQL ? ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;" : ");");

        String alliesTable = "CREATE TABLE IF NOT EXISTS clan_allies (" +
                "id INTEGER PRIMARY KEY " + autoIncrement + "," +
                "clan_name " + textType + " NOT NULL," +
                "ally_name " + textType + " NOT NULL," +
                (isMySQL ? "FOREIGN KEY (clan_name) REFERENCES clans(name) ON DELETE CASCADE," : "FOREIGN KEY (clan_name) REFERENCES clans(name) ON DELETE CASCADE,") +
                "UNIQUE(clan_name, ally_name)" +
                (isMySQL ? ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;" : ");");

        String allyRequestsTable = "CREATE TABLE IF NOT EXISTS clan_ally_requests (" +
                "id INTEGER PRIMARY KEY " + autoIncrement + "," +
                "clan_name " + textType + " NOT NULL," +
                "requesting_clan " + textType + " NOT NULL," +
                (isMySQL ? "FOREIGN KEY (clan_name) REFERENCES clans(name) ON DELETE CASCADE," : "FOREIGN KEY (clan_name) REFERENCES clans(name) ON DELETE CASCADE,") +
                "UNIQUE(clan_name, requesting_clan)" +
                (isMySQL ? ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;" : ");");

        String enemiesTable = "CREATE TABLE IF NOT EXISTS clan_enemies (" +
                "id INTEGER PRIMARY KEY " + autoIncrement + "," +
                "clan_name " + textType + " NOT NULL," +
                "enemy_name " + textType + " NOT NULL," +
                (isMySQL ? "FOREIGN KEY (clan_name) REFERENCES clans(name) ON DELETE CASCADE," : "FOREIGN KEY (clan_name) REFERENCES clans(name) ON DELETE CASCADE,") +
                "UNIQUE(clan_name, enemy_name)" +
                (isMySQL ? ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;" : ");");

        Connection conn = getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(clansTable);
            stmt.execute(membersTable);
            stmt.execute(alliesTable);
            stmt.execute(allyRequestsTable);
            stmt.execute(enemiesTable);
        } finally {
            closeConnection(conn);
        }

        ensureFriendlyFireColumn();
    }

    private void ensureFriendlyFireColumn() {
        boolean isMySQL = "MYSQL".equals(databaseType);
        String booleanType = isMySQL ? "TINYINT(1)" : "INTEGER";
        String sql = "ALTER TABLE clans ADD COLUMN friendly_fire " + booleanType + " DEFAULT 0" + (isMySQL ? ", ADD INDEX friendly_fire_idx (friendly_fire)" : "");
        Connection conn = null;
        try {
            conn = getConnection();
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (SQLException ignored) {
            // Column already exists; ignore error
        } finally {
            closeConnection(conn);
        }
    }
    
    private Connection getConnection() throws SQLException {
        if ("MYSQL".equals(databaseType)) {
            return dataSource.getConnection();
        } else {
            return connection;
        }
    }

    private void closeConnection(Connection conn) {
        if (conn != null && "MYSQL".equals(databaseType)) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.warning("Could not return MySQL connection to pool: " + e.getMessage());
            }
        }
    }

    public void saveClan(Clan clan) {
        Connection conn = null;
        try {
            conn = getConnection();
            // Insert or replace clan
            String clanSql;
            if ("MYSQL".equals(databaseType)) {
                clanSql = "INSERT INTO clans (name, leader, creation_date, description, color_code, bank_balance, friendly_fire) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                    "leader=VALUES(leader), creation_date=VALUES(creation_date), description=VALUES(description), " +
                    "color_code=VALUES(color_code), bank_balance=VALUES(bank_balance), friendly_fire=VALUES(friendly_fire)";
            } else {
                clanSql = "INSERT OR REPLACE INTO clans (name, leader, creation_date, description, color_code, bank_balance, friendly_fire) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            }
            try (PreparedStatement stmt = conn.prepareStatement(clanSql)) {
                stmt.setString(1, clan.getName());
                stmt.setString(2, clan.getLeader().toString());
                stmt.setString(3, clan.getCreationDate().toString());
                stmt.setString(4, clan.getDescription());
                stmt.setString(5, clan.getColorCode());
                stmt.setDouble(6, clan.getBankBalance());
                stmt.setBoolean(7, clan.isFriendlyFireEnabled());
                stmt.executeUpdate();
            }

            // Delete existing members and re-insert
            String deleteMembersSql = "DELETE FROM clan_members WHERE clan_name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteMembersSql)) {
                stmt.setString(1, clan.getName());
                stmt.executeUpdate();
            }

            String memberSql = "INSERT INTO clan_members (clan_name, member_uuid, join_date, kills, deaths) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(memberSql)) {
                for (UUID member : clan.getMembers()) {
                    stmt.setString(1, clan.getName());
                    stmt.setString(2, member.toString());
                    stmt.setLong(3, clan.getJoinDate(member));
                    stmt.setInt(4, clan.getMemberKills(member));
                    stmt.setInt(5, clan.getMemberDeaths(member));
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            // Delete existing allies and re-insert
            String deleteAlliesSql = "DELETE FROM clan_allies WHERE clan_name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteAlliesSql)) {
                stmt.setString(1, clan.getName());
                stmt.executeUpdate();
            }

            String allySql = "INSERT INTO clan_allies (clan_name, ally_name) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(allySql)) {
                for (String ally : clan.getAllies()) {
                    stmt.setString(1, clan.getName());
                    stmt.setString(2, ally);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            // Delete existing ally requests and re-insert
            String deleteRequestsSql = "DELETE FROM clan_ally_requests WHERE clan_name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteRequestsSql)) {
                stmt.setString(1, clan.getName());
                stmt.executeUpdate();
            }

            String requestSql = "INSERT INTO clan_ally_requests (clan_name, requesting_clan) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(requestSql)) {
                for (String request : clan.getAllyRequests()) {
                    stmt.setString(1, clan.getName());
                    stmt.setString(2, request);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            // Delete existing enemies and re-insert
            String deleteEnemiesSql = "DELETE FROM clan_enemies WHERE clan_name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteEnemiesSql)) {
                stmt.setString(1, clan.getName());
                stmt.executeUpdate();
            }

            String enemySql = "INSERT INTO clan_enemies (clan_name, enemy_name) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(enemySql)) {
                for (String enemy : clan.getEnemies()) {
                    stmt.setString(1, clan.getName());
                    stmt.setString(2, enemy);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

        } catch (SQLException e) {
            logger.severe("Could not save clan " + clan.getName() + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
    }

    public Map<String, Clan> loadAllClans() {
        Map<String, Clan> clans = new HashMap<>();

        Connection conn = null;
        try {
            conn = getConnection();
            String sql = "SELECT * FROM clans";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    String name = rs.getString("name");
                    UUID leader = UUID.fromString(rs.getString("leader"));
                    LocalDateTime creationDate = LocalDateTime.parse(rs.getString("creation_date"));
                    String description = rs.getString("description");
                    String colorCode = rs.getString("color_code");
                    double bankBalance = rs.getDouble("bank_balance");
                    boolean friendlyFire = false;
                    try {
                        friendlyFire = rs.getBoolean("friendly_fire");
                    } catch (SQLException ignored) {
                        // Column might not exist on older schemas
                    }

                    // Load members
                    Set<UUID> members = new HashSet<>();
                    Map<UUID, Long> joinDates = new HashMap<>();
                    Map<UUID, Integer> memberKills = new HashMap<>();
                    Map<UUID, Integer> memberDeaths = new HashMap<>();

                    String memberSql = "SELECT member_uuid, join_date, kills, deaths FROM clan_members WHERE clan_name = ?";
                    try (PreparedStatement memberStmt = conn.prepareStatement(memberSql)) {
                        memberStmt.setString(1, name);
                        try (ResultSet memberRs = memberStmt.executeQuery()) {
                            while (memberRs.next()) {
                                UUID memberUuid = UUID.fromString(memberRs.getString("member_uuid"));
                                long joinDate = memberRs.getLong("join_date");
                                int kills = memberRs.getInt("kills");
                                int deaths = memberRs.getInt("deaths");
                                members.add(memberUuid);
                                joinDates.put(memberUuid, joinDate);
                                memberKills.put(memberUuid, kills);
                                memberDeaths.put(memberUuid, deaths);
                            }
                        }
                    }

                    // Load allies
                    Set<String> allies = new HashSet<>();
                    String allySql = "SELECT ally_name FROM clan_allies WHERE clan_name = ?";
                    try (PreparedStatement allyStmt = conn.prepareStatement(allySql)) {
                        allyStmt.setString(1, name);
                        try (ResultSet allyRs = allyStmt.executeQuery()) {
                            while (allyRs.next()) {
                                allies.add(allyRs.getString("ally_name"));
                            }
                        }
                    }

                    // Load ally requests
                    Set<String> allyRequests = new HashSet<>();
                    String requestSql = "SELECT requesting_clan FROM clan_ally_requests WHERE clan_name = ?";
                    try (PreparedStatement requestStmt = conn.prepareStatement(requestSql)) {
                        requestStmt.setString(1, name);
                        try (ResultSet requestRs = requestStmt.executeQuery()) {
                            while (requestRs.next()) {
                                allyRequests.add(requestRs.getString("requesting_clan"));
                            }
                        }
                    }

                    // Load enemies
                    Set<String> enemies = new HashSet<>();
                    String enemySql = "SELECT enemy_name FROM clan_enemies WHERE clan_name = ?";
                    try (PreparedStatement enemyStmt = conn.prepareStatement(enemySql)) {
                        enemyStmt.setString(1, name);
                        try (ResultSet enemyRs = enemyStmt.executeQuery()) {
                            while (enemyRs.next()) {
                                enemies.add(enemyRs.getString("enemy_name"));
                            }
                        }
                    }

                    Clan clan = new Clan(name, leader, members, joinDates, creationDate, description, allies, allyRequests, memberKills, memberDeaths, enemies);
                    if (colorCode != null) {
                        clan.setColorCode(colorCode);
                    }
                    clan.setBankBalance(bankBalance);
                    clan.setFriendlyFireEnabled(friendlyFire);
                    clans.put(name.toLowerCase(), clan);
                }
            }

            logger.fine("Loaded " + clans.size() + " clans from " + databaseType + " database.");
        } catch (SQLException e) {
            logger.severe("Could not load clans from database: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }

        return clans;
    }

    public Clan getClanFromDB(String name) {
        Connection conn = null;
        try {
            conn = getConnection();
            String sql = "SELECT * FROM clans WHERE name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, name);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String clanName = rs.getString("name");
                        UUID leader = UUID.fromString(rs.getString("leader"));
                        LocalDateTime creationDate = LocalDateTime.parse(rs.getString("creation_date"));
                        String description = rs.getString("description");
                        String colorCode = rs.getString("color_code");
                        double bankBalance = rs.getDouble("bank_balance");
                        boolean friendlyFire = false;
                        try {
                            friendlyFire = rs.getBoolean("friendly_fire");
                        } catch (SQLException ignored) {
                            // Column might not exist on older schemas
                        }

                        // Load members
                        Set<UUID> members = new HashSet<>();
                        Map<UUID, Long> joinDates = new HashMap<>();
                        Map<UUID, Integer> memberKills = new HashMap<>();
                        Map<UUID, Integer> memberDeaths = new HashMap<>();

                        String memberSql = "SELECT member_uuid, join_date, kills, deaths FROM clan_members WHERE clan_name = ?";
                        try (PreparedStatement memberStmt = conn.prepareStatement(memberSql)) {
                            memberStmt.setString(1, clanName);
                            try (ResultSet memberRs = memberStmt.executeQuery()) {
                                while (memberRs.next()) {
                                    UUID memberUuid = UUID.fromString(memberRs.getString("member_uuid"));
                                    long joinDate = memberRs.getLong("join_date");
                                    int kills = memberRs.getInt("kills");
                                    int deaths = memberRs.getInt("deaths");
                                    members.add(memberUuid);
                                    joinDates.put(memberUuid, joinDate);
                                    memberKills.put(memberUuid, kills);
                                    memberDeaths.put(memberUuid, deaths);
                                }
                            }
                        }

                        // Load allies
                        Set<String> allies = new HashSet<>();
                        String allySql = "SELECT ally_name FROM clan_allies WHERE clan_name = ?";
                        try (PreparedStatement allyStmt = conn.prepareStatement(allySql)) {
                            allyStmt.setString(1, clanName);
                            try (ResultSet allyRs = allyStmt.executeQuery()) {
                                while (allyRs.next()) {
                                    allies.add(allyRs.getString("ally_name"));
                                }
                            }
                        }

                        // Load ally requests
                        Set<String> allyRequests = new HashSet<>();
                        String requestSql = "SELECT requesting_clan FROM clan_ally_requests WHERE clan_name = ?";
                        try (PreparedStatement requestStmt = conn.prepareStatement(requestSql)) {
                            requestStmt.setString(1, clanName);
                            try (ResultSet requestRs = requestStmt.executeQuery()) {
                                while (requestRs.next()) {
                                    allyRequests.add(requestRs.getString("requesting_clan"));
                                }
                            }
                        }

                        // Load enemies
                        Set<String> enemies = new HashSet<>();
                        String enemySql = "SELECT enemy_name FROM clan_enemies WHERE clan_name = ?";
                        try (PreparedStatement enemyStmt = conn.prepareStatement(enemySql)) {
                            enemyStmt.setString(1, clanName);
                            try (ResultSet enemyRs = enemyStmt.executeQuery()) {
                                while (enemyRs.next()) {
                                    enemies.add(enemyRs.getString("enemy_name"));
                                }
                            }
                        }

                        Clan clan = new Clan(clanName, leader, members, joinDates, creationDate, description, allies, allyRequests, memberKills, memberDeaths, enemies);
                        if (colorCode != null) {
                            clan.setColorCode(colorCode);
                        }
                        clan.setBankBalance(bankBalance);
                        clan.setFriendlyFireEnabled(friendlyFire);
                        return clan;
                    }
                }
            }
        } catch (SQLException e) {
            logger.severe("Could not load clan " + name + " from database: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
        return null;
    }

    public void deleteClan(String clanName) {
        Connection conn = null;
        try {
            conn = getConnection();
            String sql = "DELETE FROM clans WHERE name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, clanName);
                stmt.executeUpdate();
            }
            // Members, allies, and requests are deleted automatically via CASCADE
        } catch (SQLException e) {
            logger.severe("Could not delete clan " + clanName + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection(conn);
        }
    }

    public void close() {
        try {
            if ("MYSQL".equals(databaseType)) {
                if (dataSource != null && !dataSource.isClosed()) {
                    dataSource.close();
                    logger.info("MySQL/HikariCP connection pool closed.");
                }
            } else {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    logger.info("SQLite database connection closed.");
                }
            }
        } catch (SQLException e) {
            logger.severe("Could not close database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
