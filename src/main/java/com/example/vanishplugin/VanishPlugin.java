package com.example.vanishplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class VanishPlugin extends JavaPlugin implements Listener {

    private DatabaseManager database;
    private FileConfiguration config;
    private final Set<UUID> vanishedPlayers = new HashSet<>();
    private final Map<UUID, Integer> actionBarTasks = new HashMap<>();

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        config = getConfig();

        // Initialize database
        database = new DatabaseManager(this);
        if (database.initialize()) {
            getLogger().info("Database initialized successfully!");
        } else {
            getLogger().warning("Database failed to initialize!");
        }

        // Load vanished players from database
        loadVanishedPlayers();

        // Register events and command
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("vanish").setExecutor(new VanishCommand(this));

        getLogger().info("VanishPlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save vanished players to database
        database.saveVanishedPlayers(vanishedPlayers);

        // Cancel all action bar tasks
        for (int taskId : actionBarTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        // Close database
        database.close();

        getLogger().info("VanishPlugin has been disabled!");
    }

    private void loadVanishedPlayers() {
        Set<UUID> loaded = database.loadVanishedPlayers();
        vanishedPlayers.addAll(loaded);

        // Apply vanish to online players
        for (UUID uuid : loaded) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                applyVanish(player, false);
            }
        }

        getLogger().info("Loaded " + loaded.size() + " vanished players from database");
    }

    public boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }

    public void setVanished(Player player, boolean vanished) {
        UUID uuid = player.getUniqueId();

        if (vanished) {
            vanishedPlayers.add(uuid);
            applyVanish(player, true);
            database.setVanished(uuid, true);
            player.sendMessage(colorize("&aYou vanished!"));
        } else {
            vanishedPlayers.remove(uuid);
            removeVanish(player);
            database.setVanished(uuid, false);
            player.sendMessage(colorize("&aYou reappeared!"));
        }
    }

    private void applyVanish(Player player, boolean showActionBar) {
        // Check if world is enabled
        if (!isWorldEnabled(player)) {
            player.sendMessage(colorize("&cVanish is disabled in this world!"));
            return;
        }

        // Hide player from other players without bypass permission
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;

            if (canSeeVanished(online)) {
                online.showPlayer(player);
            } else {
                online.hidePlayer(player);
            }
        }

        // Start action bar
        if (showActionBar) {
            startActionBar(player);
        }
    }

    private void removeVanish(Player player) {
        // Show player to everyone
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(player);
        }

        // Stop action bar
        stopActionBar(player);
    }

    private void startActionBar(Player player) {
        stopActionBar(player);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline() && isVanished(player)) {
                    sendActionBar(player, colorize("&fYou are currently &cVANISHED"));
                } else {
                    stopActionBar(player);
                }
            }
        }, 0L, 20L);

        actionBarTasks.put(player.getUniqueId(), taskId);
    }

    private void stopActionBar(Player player) {
        Integer taskId = actionBarTasks.remove(player.getUniqueId());
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private void sendActionBar(Player player, String message) {
        try {
            // Get server version
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String version = packageName.substring(packageName.lastIndexOf('.') + 1);

            // Get CraftPlayer class
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            Object craftPlayer = craftPlayerClass.cast(player);

            // Get PacketPlayOutChat class
            Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
            Class<?> chatComponentClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");

            // Get ChatSerializer class and method
            Class<?> chatSerializerClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            java.lang.reflect.Method aMethod = chatSerializerClass.getDeclaredMethod("a", String.class);

            // Create JSON chat component
            String json = "{\"text\":\"" + escapeJson(message) + "\"}";
            Object chatComponent = aMethod.invoke(null, json);

            // Create packet constructor (byte 2 = action bar)
            java.lang.reflect.Constructor<?> constructor = packetClass.getConstructor(chatComponentClass, byte.class);
            Object packet = constructor.newInstance(chatComponent, (byte) 2);

            // Get player connection
            java.lang.reflect.Method getHandle = craftPlayerClass.getMethod("getHandle");
            Object entityPlayer = getHandle.invoke(craftPlayer);
            Object playerConnection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);

            // Get Packet class
            Class<?> packetBaseClass = Class.forName("net.minecraft.server." + version + ".Packet");

            // Send packet
            java.lang.reflect.Method sendPacket = playerConnection.getClass().getMethod("sendPacket", packetBaseClass);
            sendPacket.invoke(playerConnection, packet);

        } catch (Exception e) {
            // If reflection fails, send as regular message
            player.sendMessage(message);
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private boolean canSeeVanished(Player player) {
        return player.hasPermission("vanish.bypass") || player.isOp();
    }

    public boolean isWorldEnabled(Player player) {
        List<String> enabledWorlds = config.getStringList("enabled-worlds");
        String worldName = player.getWorld().getName();

        if (enabledWorlds.contains("*")) {
            return true;
        }

        return enabledWorlds.contains(worldName);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Hide vanished players from this player
        for (UUID uuid : vanishedPlayers) {
            Player vanished = Bukkit.getPlayer(uuid);
            if (vanished != null && vanished.isOnline() && !canSeeVanished(player)) {
                player.hidePlayer(vanished);
            }
        }

        // If this player was vanished, restore it
        if (isVanished(player)) {
            applyVanish(player, false);
            player.sendMessage(colorize("&aYour vanish has been restored!"));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopActionBar(event.getPlayer());
    }

    public String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}