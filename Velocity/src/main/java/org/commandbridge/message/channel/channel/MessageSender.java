package org.commandbridge.message.channel.channel;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.commandbridge.CommandBridge;
import org.commandbridge.utilities.VerboseLogger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MessageSender {
    private final ProxyServer server;
    private final VerboseLogger verboseLogger;

    public MessageSender(ProxyServer server, CommandBridge plugin) {
        this.server = server;
        this.verboseLogger = plugin.getVerboseLogger();
    }


    public void sendCommandToBukkit(String command, String targetServerId, String targetExecutor) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);
        try {
            out.writeUTF("ExecuteCommand");
            out.writeUTF(targetServerId);
            out.writeUTF(targetExecutor);
            out.writeUTF(command);
            server.getAllServers().stream()
                    .filter(serverConnection -> serverConnection.getServerInfo().getName().equals(targetServerId))
                    .forEach(serverConnection -> {
                        serverConnection.sendPluginMessage(MinecraftChannelIdentifier.create("commandbridge", "main"), byteOut.toByteArray());
                        verboseLogger.info("Command sent to Bukkit server " + targetServerId + ": " + command);
                    });
        } catch (IOException e) {
            verboseLogger.error("Failed to send command to Bukkit", e);
        }
    }

    public void sendSystemCommand(String command) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);
        try {
            out.writeUTF("SystemCommand");
            out.writeUTF(command);
            server.getAllServers().forEach(serverConnection -> {
                serverConnection.sendPluginMessage(MinecraftChannelIdentifier.create("commandbridge", "main"), byteOut.toByteArray());
                verboseLogger.info("System command sent to Bukkit server: " + command);
            });
        } catch (IOException e) {
            verboseLogger.error("Failed to send system command to Bukkit", e);
        }
    }
}
