package com.example.vanishplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VanishCommand implements CommandExecutor {

    private final VanishPlugin plugin;

    public VanishCommand(VanishPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        // Check permission
        if (!player.hasPermission("vanish.use") && !player.isOp()) {
            player.sendMessage(plugin.colorize("&cYou don't have permission!"));
            return true;
        }

        // Check world
        if (!plugin.isWorldEnabled(player)) {
            player.sendMessage(plugin.colorize("&cVanish is disabled in this world!"));
            return true;
        }

        // Toggle vanish
        boolean isVanished = plugin.isVanished(player);
        plugin.setVanished(player, !isVanished);

        return true;
    }
}