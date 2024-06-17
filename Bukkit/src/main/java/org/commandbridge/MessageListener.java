package org.commandbridge;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;


public class MessageListener implements PluginMessageListener {
    private final CommandBridge plugin;

    public MessageListener(CommandBridge plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        VerboseLogger logger = new VerboseLogger(plugin);
        logger.info("Received plugin message on channel " + channel);
        if (!"commandbridge:main".equals(channel)) return;
        try (ByteArrayInputStream stream = new ByteArrayInputStream(message);
             DataInputStream in = new DataInputStream(stream)) {
            String subChannel = in.readUTF();
            if ("ExecuteCommand".equals(subChannel)) {
                String targetServerId = in.readUTF();
                String targetExecutor = in.readUTF();
                String command = in.readUTF();
                logger.info("Received command to execute on server " + targetServerId + " as " + targetExecutor + ": " + command);

                if (!targetServerId.equals(plugin.getConfig().getString("server-id"))) {
                    logger.info("Command not for this server, ignoring.");
                    return;
                }

                if ("player".equals(targetExecutor)) {
                    logger.info("Executing command as player: " + command);
                    Bukkit.dispatchCommand(player, command);
                } else if ("console".equals(targetExecutor)) {
                    logger.info("Executing command as console: " + command);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            } else if ("RegisterCommand".equals(subChannel)) {
                String targetServerId = in.readUTF();
                String command = in.readUTF();
                logger.info("Received command to register on server " + targetServerId + ": " + command);

                if (!targetServerId.equals(plugin.getConfig().getString("server-id"))) {
                    logger.info("Command not for this server, ignoring.");
                    return;
                }

                plugin.getLogger().info("Register Command .... : " + command);
                plugin.getCommandRegister().registerCommand(command);

            }
            if ("UnregisterCommand".equals(subChannel)) {
                String targetServerId = in.readUTF();
                String command = in.readUTF();
                logger.info("Received command to unregister on server " + targetServerId + ": " + command);

                if (!targetServerId.equals(plugin.getConfig().getString("server-id"))) {
                    logger.info("Command not for this server, ignoring.");
                    return;
                }

                plugin.getLogger().info("Unregister Command .... : " + command);
                plugin.getCommandRegister().unregisterCommand(command);
            }
        } catch (IOException e) {
            logger.error("Failed to read plugin message" , e);
        }
    }

}