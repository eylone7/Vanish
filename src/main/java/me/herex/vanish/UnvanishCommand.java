package me.herex.vanish;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnvanishCommand implements CommandExecutor {

    private final VanishManager vanishManager;

    public UnvanishCommand(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        // Check if player is vanished
        if (!vanishManager.isVanished(player)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou are not Vanished!"));
            return true;
        }

        // Unvanish player - this will handle the message internally
        vanishManager.unvanishPlayer(player);
        return true;
    }
}