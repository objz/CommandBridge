package org.commandbridge.message.channel;

import org.bukkit.entity.Player;
import org.commandbridge.CommandBridge;
import org.commandbridge.utilities.VerboseLogger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class MessageSender {
    private final CommandBridge plugin;
    private final VerboseLogger verboseLogger;

    public MessageSender(CommandBridge plugin) {
        this.plugin = plugin;
        this.verboseLogger = plugin.getVerboseLogger();
    }

    public void sendPluginMessage(Player player, String executor, String command) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);
        String playerUUID = player.getUniqueId().toString();
        verboseLogger.info("Sending plugin message for command as Player: " + command + " to " + executor + " as " + playerUUID);
        try {
            out.writeUTF("ExecuteCommand");
            out.writeUTF(executor);
            out.writeUTF(playerUUID);
            out.writeUTF(command);
            player.sendPluginMessage(plugin, "commandbridge:main", byteOut.toByteArray());
            verboseLogger.info("Plugin message sent to Velocity server: " + command);
        } catch (Exception e) {
            verboseLogger.error("Failed to send plugin message", e);
        }

    }
}
