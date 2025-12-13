package com.example.vanishplugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class ActionBarUtils {

    public static void sendActionBar(Player player, String message) {
        try {
            // Try modern method first (1.12+)
            Class<?> spigotPlayerClass = Class.forName("org.bukkit.entity.Player$Spigot");
            Method sendMessageMethod = spigotPlayerClass.getMethod("sendMessage",
                    Class.forName("net.md_5.bungee.api.ChatMessageType"),
                    Class.forName("net.md_5.bungee.api.chat.BaseComponent[]"));

            Object spigot = player.getClass().getMethod("spigot").invoke(player);

            Class<?> chatMessageTypeClass = Class.forName("net.md_5.bungee.api.ChatMessageType");
            Object actionBar = chatMessageTypeClass.getField("ACTION_BAR").get(null);

            Class<?> baseComponentClass = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
            Class<?> textComponentClass = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Constructor<?> textComponentConstructor = textComponentClass.getConstructor(String.class);
            Object textComponent = textComponentConstructor.newInstance(message);
            Object[] components = new Object[]{textComponent};

            sendMessageMethod.invoke(spigot, actionBar, components);
            return;

        } catch (Exception e) {
            // Modern method failed, try legacy method
            try {
                String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

                Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
                Object craftPlayer = craftPlayerClass.cast(player);

                Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
                Class<?> chatComponentClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");

                Class<?> chatSerializerClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
                Method aMethod = chatSerializerClass.getDeclaredMethod("a", String.class);
                Object chatComponent = aMethod.invoke(null, "{\"text\":\"" + cleanJson(message) + "\"}");

                Constructor<?> constructor = packetClass.getConstructor(chatComponentClass, byte.class);
                Object packet = constructor.newInstance(chatComponent, (byte) 2);

                Method getHandleMethod = craftPlayerClass.getMethod("getHandle");
                Object entityPlayer = getHandleMethod.invoke(craftPlayer);
                Object playerConnection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);
                Method sendPacketMethod = playerConnection.getClass().getMethod("sendPacket",
                        Class.forName("net.minecraft.server." + version + ".Packet"));
                sendPacketMethod.invoke(playerConnection, packet);
                return;

            } catch (Exception e2) {
                // Both methods failed, send as chat message
                player.sendMessage(message);
            }
        }
    }

    private static String cleanJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}