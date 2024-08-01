package org.commandbridge.message.channel;

import org.bukkit.entity.Player;
import org.commandbridge.CommandBridge;
import org.commandbridge.utilities.VerboseLogger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

public class MessageSender {
    private final CommandBridge plugin;
    private final VerboseLogger verboseLogger;

    public MessageSender(CommandBridge plugin) {
        this.plugin = plugin;
        this.verboseLogger = plugin.getVerboseLogger();
    }

    public void sendPluginMessage(String playerUUID, String executor, String command, String targetVelocityServer) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);
        verboseLogger.info("Preparing to send plugin message: Command = " + command + ", Executor = " + executor + ", PlayerUUID = " + playerUUID);

        try {
            out.writeUTF("ExecuteCommand");
            out.writeUTF(targetVelocityServer);
            out.writeUTF(executor);
            out.writeUTF(playerUUID);
            out.writeUTF(command);



            if ("player".equals(executor)) {
                Player player = plugin.getServer().getPlayer(UUID.fromString(playerUUID));
                if (player != null && player.isOnline()) {
                    player.sendPluginMessage(plugin, "commandbridge:main", byteOut.toByteArray());
                    verboseLogger.forceInfo("Plugin message sent successfully to player: Command = " + command + ", Executor = " + executor + ", PlayerUUID = " + playerUUID);
                } else {
                    verboseLogger.warn("Player with UUID " + playerUUID + " not found or not online.");
                }
            } else if ("console".equals(executor)) {
                Collection<? extends Player> onlinePlayers = plugin.getServer().getOnlinePlayers();
                if (!onlinePlayers.isEmpty()) {
                    Player targetPlayer = onlinePlayers.iterator().next();
                    targetPlayer.sendPluginMessage(plugin, "commandbridge:main", byteOut.toByteArray());
                    verboseLogger.forceInfo("Plugin message sent successfully from player " + targetPlayer.getName() + " on behalf of console: Command = " + command + ", Executor = " + executor);
                } else {
                    verboseLogger.warn("No players online to send the plugin message.");
                }
            }
        } catch (IOException e) {
            verboseLogger.error("Failed to send plugin message: Command = " + command + ", Executor = " + executor + ", PlayerUUID = " + playerUUID, e);
        }
    }

    public void sendVersion(String version) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        verboseLogger.info("Preparing to send version: " + version);

        try {
            dataOutputStream.writeUTF("Version");
            dataOutputStream.writeUTF(version);

            Collection<? extends Player> onlinePlayers = plugin.getServer().getOnlinePlayers();
            if (!onlinePlayers.isEmpty()) {
                Player targetPlayer = onlinePlayers.iterator().next();
                targetPlayer.sendPluginMessage(plugin, "commandbridge:main", byteArrayOutputStream.toByteArray());
                verboseLogger.info("Plugin Version sent successfully to player: " + targetPlayer.getName() + " on behalf of version: " + version);
            } else {
                verboseLogger.warn("No players online to send the plugin message.");
            }
            verboseLogger.forceInfo("Version message sent successfully: " + version);
        } catch (IOException e) {
            verboseLogger.error("Failed to send version: " + version, e);
        }
    }
}
