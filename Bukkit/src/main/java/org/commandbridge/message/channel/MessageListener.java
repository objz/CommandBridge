package org.commandbridge.message.channel;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.commandbridge.CommandBridge;
import org.commandbridge.utilities.VerboseLogger;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class MessageListener implements PluginMessageListener {
    private final CommandBridge plugin;
    private final VerboseLogger verboseLogger;

    public MessageListener(CommandBridge plugin) {
        this.plugin = plugin;
        this.verboseLogger = plugin.getVerboseLogger();
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        verboseLogger.info("Received plugin message on channel: " + channel);

        if (!"commandbridge:main".equals(channel)) {
            verboseLogger.warn("Received message on unknown channel: " + channel);
            return;
        }

        try (ByteArrayInputStream stream = new ByteArrayInputStream(message);
             DataInputStream in = new DataInputStream(stream)) {

            String subChannel = in.readUTF();
            verboseLogger.info("SubChannel: " + subChannel);

            switch (subChannel) {
                case "ExecuteCommand":
                    handleExecuteCommand(in, player);
                    break;
                case "SystemCommand":
                    handleSystemCommand(in);
                    break;
                default:
                    verboseLogger.warn("Unknown subChannel: " + subChannel);
                    break;
            }

        } catch (IOException e) {
            verboseLogger.error("Failed to read plugin message", e);
        }
    }

    private void handleExecuteCommand(DataInputStream in, Player player) throws IOException {
        String targetServerId = in.readUTF();
        String targetExecutor = in.readUTF();
        String command = in.readUTF();
        verboseLogger.info("Received command to execute on server " + targetServerId + " as " + targetExecutor + ": " + command);

        if (!targetServerId.equals(plugin.getConfig().getString("server-id"))) {
            verboseLogger.info("Command not for this server, ignoring.");
            return;
        }

        switch (targetExecutor) {
            case "player":
                verboseLogger.info("Executing command as player: " + command);
                Bukkit.dispatchCommand(player, command);
                break;
            case "console":
                verboseLogger.info("Executing command as console: " + command);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                break;
            default:
                verboseLogger.warn("Unknown target executor: " + targetExecutor);
                break;
        }
    }

    private void handleSystemCommand(DataInputStream in) throws IOException {
        String command = in.readUTF();
        verboseLogger.info("Received System Command: " + command);

        switch (command) {
            case "reload":
                plugin.reloadConfig();
                plugin.getScripts().loadScripts();
                verboseLogger.info("Reloaded configuration and scripts.");
                break;
            case "version":
                String version = plugin.getBukkitVersion();
                plugin.getMessageSender().sendVersion(version);
                verboseLogger.info("Sent plugin version: " + version);
                break;
            default:
                verboseLogger.warn("Unknown system command: " + command);
                break;
        }
    }
}
