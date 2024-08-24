package org.commandbridge.core.message.channel;

import org.commandbridge.CommandBridge;
import org.commandbridge.core.utilities.VerboseLogger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
        String uuid = UUID.randomUUID().toString();
        verboseLogger.info("Preparing to send plugin message: Command = " + command + ", Executor = " + executor + ", PlayerUUID = " + playerUUID);

        try {
            out.writeUTF("ExecuteCommand");
            out.writeUTF(targetVelocityServer);
            out.writeUTF(uuid);
            out.writeUTF(executor);
            out.writeUTF(playerUUID);
            out.writeUTF(command);



            if ("player".equals(executor)) {
                plugin.getServer().sendPluginMessage(plugin, "commandbridge:main", byteOut.toByteArray());
                verboseLogger.forceInfo("Plugin message sent successfully to player: Command = " + command + ", Executor = " + executor + ", PlayerUUID = " + playerUUID + ", UUID = " + uuid);
            } else if ("console".equals(executor)) {

                plugin.getServer().sendPluginMessage(plugin, "commandbridge:main", byteOut.toByteArray());
                verboseLogger.forceInfo("Plugin message sent successfully from console: Command = " + command + ", Executor = " + executor + ", UUID = " + uuid);
            }
        } catch (IOException e) {
            verboseLogger.error("Failed to send plugin message: Command = " + command + ", Executor = " + executor + ", PlayerUUID = " + playerUUID, e);
        }
    }

    public void sendVersion(String version) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        String uuid = UUID.randomUUID().toString();
        verboseLogger.info("Preparing to send version: " + version);

        try {
            dataOutputStream.writeUTF("Version");
            dataOutputStream.writeUTF(uuid);
            dataOutputStream.writeUTF(version);


            plugin.getServer().sendPluginMessage(plugin, "commandbridge:main", byteArrayOutputStream.toByteArray());
            verboseLogger.forceInfo("Version message sent successfully: " + version);
        } catch (IOException e) {
            verboseLogger.error("Failed to send version: " + version, e);
        }
    }
}
