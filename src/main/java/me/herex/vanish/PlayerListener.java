package me.herex.vanish;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final VanishManager vanishManager;
    private final DatabaseManager databaseManager;

    public PlayerListener(VanishManager vanishManager, DatabaseManager databaseManager) {
        this.vanishManager = vanishManager;
        this.databaseManager = databaseManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        vanishManager.handlePlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save vanish state on quit
        if (vanishManager.isVanished(event.getPlayer())) {
            databaseManager.setPlayerVanished(
                    event.getPlayer().getUniqueId(),
                    event.getPlayer().getName(),
                    true
            );
        }
    }
}