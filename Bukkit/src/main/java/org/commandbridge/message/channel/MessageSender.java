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
        verboseLogger.info("Preparing to send plugin message: Command = " + command + ", Executor = " + executor + ", PlayerUUID = " + playerUUID);

        try {
            out.writeUTF("ExecuteCommand");
            out.writeUTF(executor);
            out.writeUTF(playerUUID);
            out.writeUTF(command);

            plugin.getServer().sendPluginMessage(plugin, "commandbridge:main", byteOut.toByteArray());
            verboseLogger.forceInfo("Plugin message sent successfully: Command = " + command + ", Executor = " + executor + ", PlayerUUID = " + playerUUID);
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

            plugin.getServer().sendPluginMessage(plugin, "commandbridge:main", byteArrayOutputStream.toByteArray());
            verboseLogger.forceInfo("Version message sent successfully: " + version);
        } catch (IOException e) {
            verboseLogger.error("Failed to send version: " + version, e);
        }
    }
}
