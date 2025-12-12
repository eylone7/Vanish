package me.herex.vanish;

import org.bukkit.configuration.file.FileConfiguration;
import java.sql.*;
import java.util.UUID;

public class DatabaseManager {

    private final VanishPlugin plugin;
    private Connection connection;

    public DatabaseManager(VanishPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            FileConfiguration config = plugin.getConfig();
            String databaseType = config.getString("database.type", "sqlite").toLowerCase();

            if (databaseType.equals("mysql")) {
                // MySQL connection
                String host = config.getString("database.mysql.host", "localhost");
                int port = config.getInt("database.mysql.port", 3306);
                String database = config.getString("database.mysql.database", "vanishdb");
                String username = config.getString("database.mysql.username", "root");
                String password = config.getString("database.mysql.password", "");

                String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                        "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

                plugin.getLogger().info("Connecting to MySQL database...");
                connection = DriverManager.getConnection(url, username, password);
                plugin.getLogger().info("Successfully connected to MySQL database!");

            } else {
                // SQLite connection (default)
                String path = config.getString("database.sqlite.path", "plugins/VanishPlugin/vanish.db");
                String url = "jdbc:sqlite:" + path;

                plugin.getLogger().info("Using SQLite database...");
                connection = DriverManager.getConnection(url);
                plugin.getLogger().info("Successfully connected to SQLite database!");
            }

            createTables();

        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to connect to database. Vanish persistence will not work.");
            plugin.getLogger().warning("Error: " + e.getMessage());
            connection = null;
        }
    }

    private void createTables() throws SQLException {
        if (connection == null) return;

        String sql = "CREATE TABLE IF NOT EXISTS vanished_players (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "player_name VARCHAR(16)," +
                "vanished BOOLEAN DEFAULT 0," +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public boolean isPlayerVanished(UUID uuid) {
        if (connection == null) return false;

        String sql = "SELECT vanished FROM vanished_players WHERE uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getBoolean("vanished");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking if player is vanished: " + e.getMessage());
        }
        return false;
    }

    public void setPlayerVanished(UUID uuid, String playerName, boolean vanished) {
        if (connection == null) return;

        String sql = "INSERT INTO vanished_players (uuid, player_name, vanished) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE player_name = ?, vanished = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, playerName);
            pstmt.setBoolean(3, vanished);
            pstmt.setString(4, playerName);
            pstmt.setBoolean(5, vanished);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().warning("Error setting player vanished state: " + e.getMessage());
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error closing database connection: " + e.getMessage());
        }
    }
}