package com.example.vanishplugin;

import java.io.File;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final VanishPlugin plugin;
    private Connection connection;

    public DatabaseManager(VanishPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        try {
            String dbType = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();

            if (dbType.equals("mysql")) {
                return setupMySQL();
            } else {
                return setupSQLite();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            return false;
        }
    }

    private boolean setupSQLite() throws Exception {
        // Create plugin directory
        plugin.getDataFolder().mkdirs();

        // Get database path
        String path = plugin.getConfig().getString("database.sqlite.path", "plugins/VanishPlugin/vanish.db");
        File dbFile = new File(path);

        // Create parent directories if needed
        if (dbFile.getParentFile() != null && !dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
        }

        // Load SQLite driver
        Class.forName("org.sqlite.JDBC");

        // Create connection
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);

        // Create table
        String sql = "CREATE TABLE IF NOT EXISTS vanished_players (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "vanished BOOLEAN NOT NULL, " +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }

        plugin.getLogger().info("SQLite database initialized: " + path);
        return true;
    }

    private boolean setupMySQL() throws Exception {
        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("database.mysql.port", 3306);
        String database = plugin.getConfig().getString("database.mysql.database", "vanishdb");
        String username = plugin.getConfig().getString("database.mysql.username", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "");

        // Load MySQL driver
        Class.forName("com.mysql.jdbc.Driver");

        // Create connection
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false";
        connection = DriverManager.getConnection(url, username, password);

        // Create table
        String sql = "CREATE TABLE IF NOT EXISTS vanished_players (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "vanished BOOLEAN NOT NULL, " +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }

        plugin.getLogger().info("MySQL database connected: " + database);
        return true;
    }

    public void setVanished(UUID uuid, boolean vanished) {
        String sql = "INSERT OR REPLACE INTO vanished_players (uuid, vanished) VALUES (?, ?)";

        // Check if MySQL
        try {
            if (connection.getMetaData().getURL().contains("mysql")) {
                sql = "REPLACE INTO vanished_players (uuid, vanished) VALUES (?, ?)";
            }
        } catch (SQLException e) {
            // Use default SQL
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setBoolean(2, vanished);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to set vanish for " + uuid, e);
        }
    }

    public Set<UUID> loadVanishedPlayers() {
        Set<UUID> vanished = new HashSet<>();
        String sql = "SELECT uuid FROM vanished_players WHERE vanished = 1";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                try {
                    vanished.add(UUID.fromString(rs.getString("uuid")));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in database: " + rs.getString("uuid"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load vanished players", e);
        }

        return vanished;
    }

    public void saveVanishedPlayers(Set<UUID> vanishedPlayers) {
        try {
            // Start transaction
            connection.setAutoCommit(false);

            // Clear all vanish status
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("UPDATE vanished_players SET vanished = 0");
            }

            // Save current vanished players
            for (UUID uuid : vanishedPlayers) {
                setVanished(uuid, true);
            }

            // Commit transaction
            connection.commit();
            connection.setAutoCommit(true);

        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to rollback transaction", ex);
            }
            plugin.getLogger().log(Level.SEVERE, "Failed to save vanished players", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to close database connection", e);
        }
    }
}