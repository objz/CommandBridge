package org.commandbridge.message.channel;


import org.commandbridge.CommandBridge;
import org.commandbridge.utilities.VerboseLogger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MessageSender {
    private final CommandBridge plugin;
    private final VerboseLogger verboseLogger;

    public MessageSender(CommandBridge plugin) {
        this.plugin = plugin;
        this.verboseLogger = plugin.getVerboseLogger();
    }

    public void sendPluginMessage(String playerUUID, String executor, String command) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);
        verboseLogger.info("Sending plugin message for command as Player: " + command + " to " + executor + " as " + playerUUID);
        try {
            out.writeUTF("ExecuteCommand");
            out.writeUTF(executor);
            out.writeUTF(playerUUID);
            out.writeUTF(command);
            plugin.getServer().sendPluginMessage(plugin, "commandbridge:main", byteOut.toByteArray());
            verboseLogger.info("Plugin message sent to Velocity server: " + command);
        } catch (Exception e) {
            verboseLogger.error("Failed to send plugin message", e);
        }

    }

    public void sendVersion(String version) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        verboseLogger.info("Sending version to velocity: " + version);
        try {
            dataOutputStream.writeUTF("Version");
            dataOutputStream.writeUTF(version);
            plugin.getServer().sendPluginMessage(plugin, "commandbridge:main", byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            verboseLogger.error("Failed to send version: ", e);
        }
    }
}
