package me.herex.vanish;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static me.herex.vanish.ActionBarUtil.sendActionBar;

public class VanishManager {

    private final VanishPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Set<UUID> vanishedPlayers = new HashSet<>();

    public VanishManager(VanishPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }

    public void vanishPlayer(Player player) {
        try {
            // Check permission
            if (!player.hasPermission("vanish.use")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use vanish!");
                return;
            }

            // Check if already vanished
            if (isVanished(player)) {
                player.sendMessage(ChatColor.YELLOW + "You are already vanished!");
                return;
            }

            // Add to vanished list
            vanishedPlayers.add(player.getUniqueId());

            // Save to database
            if (databaseManager != null) {
                databaseManager.setPlayerVanished(player.getUniqueId(), player.getName(), true);
            }

            // Hide player from others
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer != player && !onlinePlayer.hasPermission("vanish.view")) {
                    onlinePlayer.hidePlayer(plugin, player);
                }
            }

            // Send vanish message - FIXED: Use proper color codes
            player.sendMessage(ChatColor.GREEN + "You vanished!");

            // Log
            plugin.getLogger().info(player.getName() + " has vanished");

        } catch (Exception e) {
            plugin.getLogger().severe("Error in vanishPlayer for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatColor.RED + "An error occurred while vanishing!");
            e.printStackTrace();
        }
    }

    public void unvanishPlayer(Player player) {
        try {
            // Check if actually vanished
            if (!isVanished(player)) {
                player.sendMessage(ChatColor.RED + "You are not vanished!");
                return;
            }

            // Remove from vanished list
            vanishedPlayers.remove(player.getUniqueId());

            // Save to database
            if (databaseManager != null) {
                databaseManager.setPlayerVanished(player.getUniqueId(), player.getName(), false);
            }

            // Show player to everyone
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.canSee(player)) {
                    onlinePlayer.showPlayer(plugin, player);
                }
            }

            // Send unvanish message - FIXED: Use proper color codes
            player.sendMessage(ChatColor.GREEN + "You reappeared!");

            // Log
            plugin.getLogger().info(player.getName() + " has reappeared");

        } catch (Exception e) {
            plugin.getLogger().severe("Error in unvanishPlayer for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatColor.RED + "An error occurred while unvanishing!");
            e.printStackTrace();
        }
    }

    public void toggleVanish(Player player) {
        if (isVanished(player)) {
            unvanishPlayer(player);
        } else {
            vanishPlayer(player);
        }
    }

    public void handlePlayerJoin(Player player) {
        try {
            // Check database if player was vanished before
            boolean wasVanished = false;
            if (databaseManager != null) {
                wasVanished = databaseManager.isPlayerVanished(player.getUniqueId());
            }

            if (wasVanished && player.hasPermission("vanish.use")) {
                vanishedPlayers.add(player.getUniqueId());

                // Hide from other players
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (onlinePlayer != player && !onlinePlayer.hasPermission("vanish.view")) {
                        onlinePlayer.hidePlayer(plugin, player);
                    }
                }

                player.sendMessage(ChatColor.GREEN + "Your vanish has been restored!");
                plugin.getLogger().info(player.getName() + "'s vanish was restored on join");
            }

            // Hide vanished players from joining player
            if (!player.hasPermission("vanish.view")) {
                for (UUID vanishedId : vanishedPlayers) {
                    Player vanishedPlayer = Bukkit.getPlayer(vanishedId);
                    if (vanishedPlayer != null && vanishedPlayer.isOnline() && !vanishedPlayer.equals(player)) {
                        player.hidePlayer(plugin, vanishedPlayer);
                    }
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error handling player join for " + player.getName() + ": " + e.getMessage());
        }
    }

    public void saveAllVanishedPlayers() {
        if (databaseManager != null) {
            for (UUID uuid : vanishedPlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    databaseManager.setPlayerVanished(uuid, player.getName(), true);
                }
            }
        }
    }

    public void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : vanishedPlayers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline() && player.isValid()) {
                        // Send action bar message
                        sendActionBar(player, ChatColor.translateAlternateColorCodes('&', "&fYou are currently &cVANISHED"));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Run every second (20 ticks)
    }
}