package org.commandbridge;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Bridge {
    private final ProxyServer server;
    private final VerboseLogger verboseLogger;

    public Bridge(ProxyServer server, CommandBridge plugin) {
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

    public void registerBukkitCommand(String command, String targetServerId) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);
        try {
            out.writeUTF("RegisterCommand");
            out.writeUTF(targetServerId);
            out.writeUTF(command);
            server.getAllServers().stream()
                    .filter(serverConnection -> serverConnection.getServerInfo().getName().equals(targetServerId))
                    .forEach(serverConnection -> {
                        serverConnection.sendPluginMessage(MinecraftChannelIdentifier.create("commandbridge", "main"), byteOut.toByteArray());
                        verboseLogger.info("Command registered on Bukkit server " + targetServerId + ": " + command);
                    });
        } catch (IOException e) {
            verboseLogger.error("Failed to register command on Bukkit", e);
        }
    }
}
