package me.herex.vanish;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class ActionBarUtil {

    public static void sendActionBar(Player player, String message) {
        if (!player.isOnline() || message == null || message.isEmpty()) {
            return;
        }

        try {
            // Get CraftPlayer
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + getServerVersion() + ".entity.CraftPlayer");
            Object craftPlayer = craftPlayerClass.cast(player);

            // Get handle
            Object handle = craftPlayerClass.getMethod("getHandle").invoke(craftPlayer);

            // Create IChatBaseComponent
            Class<?> chatComponentClass = Class.forName("net.minecraft.server." + getServerVersion() + ".IChatBaseComponent");
            Class<?> chatSerializerClass = Class.forName("net.minecraft.server." + getServerVersion() + ".IChatBaseComponent$ChatSerializer");
            Method aMethod = chatSerializerClass.getMethod("a", String.class);

            // Format message with colors
            String jsonMessage = "{\"text\":\"" + ChatColor.translateAlternateColorCodes('&', message).replace("\"", "\\\"") + "\"}";
            Object chatComponent = aMethod.invoke(null, jsonMessage);

            // Create packet
            Class<?> packetClass = Class.forName("net.minecraft.server." + getServerVersion() + ".PacketPlayOutChat");
            Object packet;

            try {
                // Try 1.12+ method
                Class<?> chatMessageTypeClass = Class.forName("net.minecraft.server." + getServerVersion() + ".ChatMessageType");
                Object[] types = chatMessageTypeClass.getEnumConstants();
                Object actionBarType = null;

                for (Object type : types) {
                    if (type.toString().equals("GAME_INFO")) {
                        actionBarType = type;
                        break;
                    }
                }

                if (actionBarType != null) {
                    Constructor<?> constructor = packetClass.getConstructor(chatComponentClass, chatMessageTypeClass);
                    packet = constructor.newInstance(chatComponent, actionBarType);
                } else {
                    throw new Exception("GAME_INFO type not found");
                }
            } catch (Exception e) {
                // 1.8-1.11 method
                Constructor<?> constructor = packetClass.getConstructor(chatComponentClass, byte.class);
                packet = constructor.newInstance(chatComponent, (byte) 2);
            }

            // Send packet
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            Method sendPacketMethod = playerConnection.getClass().getMethod("sendPacket",
                    Class.forName("net.minecraft.server." + getServerVersion() + ".Packet"));
            sendPacketMethod.invoke(playerConnection, packet);

        } catch (Exception e) {
            // Fallback: Use titles (shows at top of screen)
            try {
                player.sendTitle("", ChatColor.translateAlternateColorCodes('&', message), 0, 40, 10);
            } catch (Exception e2) {
                // Last fallback: Chat message
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }
        }
    }

    private static String getServerVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    }
}