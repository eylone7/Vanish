package me.herex.vanish;

import org.bukkit.plugin.java.JavaPlugin;

public class VanishPlugin extends JavaPlugin {

    private static VanishPlugin instance;
    private VanishManager vanishManager;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Initialize database
        databaseManager = new DatabaseManager(this);
        try {
            databaseManager.initialize();
        } catch (Exception e) {
            getLogger().warning("Database initialization failed: " + e.getMessage());
        }

        // Initialize manager
        vanishManager = new VanishManager(this, databaseManager);

        // Register commands
        getCommand("vanish").setExecutor(new VanishCommand(vanishManager));
        getCommand("unvanish").setExecutor(new UnvanishCommand(vanishManager));

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(vanishManager, databaseManager), this);

        // Start action bar task
        vanishManager.startActionBarTask();

        getLogger().info("VanishPlugin has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (vanishManager != null) {
            vanishManager.saveAllVanishedPlayers();
        }

        if (databaseManager != null) {
            databaseManager.closeConnection();
        }

        getLogger().info("VanishPlugin has been disabled!");
    }

    public static VanishPlugin getInstance() {
        return instance;
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}