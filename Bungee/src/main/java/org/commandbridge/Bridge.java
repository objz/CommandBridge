package org.commandbridge;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Bridge {
    private final ProxyServer proxy;
    private final Plugin plugin;

    public Bridge(Plugin plugin) {
        this.proxy = ProxyServer.getInstance();
        this.plugin = plugin;
    }

    public void sendCommandToBukkit(String command, String targetServerId, String targetExecutor) {
        ServerInfo targetServer = proxy.getServerInfo(targetServerId);
        if (targetServer != null) {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteOut);
            try {
                out.writeUTF("ExecuteCommand");
                out.writeUTF(targetServerId);
                out.writeUTF(targetExecutor);
                out.writeUTF(command);

                targetServer.sendData("commandbridge:main", byteOut.toByteArray());
                plugin.getLogger().info("Command sent to Bukkit server " + targetServerId + ": " + command);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to send command to Bukkit: " + e.getMessage());
            }
        } else {
            plugin.getLogger().severe("Server not found: " + targetServerId);
        }
    }
}
