package org.commandbridge.message.channel.channel;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.commandbridge.CommandBridge;
import org.commandbridge.utilities.VerboseLogger;
import org.commandbridge.utilities.VersionChecker;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class MessageListener {
    private final CommandBridge plugin;
    private final VerboseLogger logger;
    private final ProxyServer proxyServer;

    @Inject
    public MessageListener(CommandBridge plugin, ProxyServer proxyServer) {
        this.plugin = plugin;
        this.logger = plugin.getVerboseLogger();
        this.proxyServer = proxyServer;
    }

    @Subscribe
    public void onPluginMessageReceived(PluginMessageEvent event) {
        logger.info("Received plugin message on channel " + event.getIdentifier().getId());

        if (!event.getIdentifier().equals(plugin.getChannelIdentifier())) {
            logger.warn("Message received on unrecognized channel: " + event.getIdentifier().getId());
            return;
        }

        logger.info("Received valid plugin message on channel " + plugin.getChannelIdentifier().getId());
        decodePluginMessage(event.getData());
    }

    private void decodePluginMessage(byte[] data) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
             DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream)) {

            if (dataInputStream.available() == 0) {
                logger.warn("No data available in the plugin message.");
                return;
            }

            String messageType = dataInputStream.readUTF();
            logger.info("Message type: " + messageType);

            switch (messageType) {
                case "ExecuteCommand" -> {
                    String executor = dataInputStream.readUTF();
                    logger.info("Executor type: " + executor);

                    if (executor.equals("player")) {
                        String playerUUID = dataInputStream.readUTF();  // Lesen Sie die UUID als UTF-String
                        String command = dataInputStream.readUTF();
                        logger.info("Received command from player UUID: " + playerUUID + ", command: " + command);

                        Optional<Player> player = proxyServer.getPlayer(UUID.fromString(playerUUID));
                        if (player.isPresent()) {
                            Player targetPlayer = player.get();
                            proxyServer.getCommandManager().executeAsync(targetPlayer, command);
                            logger.info("Executed command as player: " + command);
                        } else {
                            logger.warn("Player not found with UUID: " + playerUUID);
                        }
                    } else if (executor.equals("console")) {
                        String command = dataInputStream.readUTF();
                        logger.info("Received command for console: " + command);

                        proxyServer.getCommandManager().executeAsync(proxyServer.getConsoleCommandSource(), command);
                        logger.info("Executed command as console: " + command);
                    } else {
                        logger.warn("Invalid executor: " + executor);
                    }
                }
                case "SystemCommand" -> {
                    String command = dataInputStream.readUTF();
                    logger.info("Received system command: " + command);
                }
                case "Version" -> {
                    String version = dataInputStream.readUTF();
                    plugin.getStartup().isSameBukkitVersion(VersionChecker.checkBukkitVersion(version, plugin.getVersion()));
                }
                default -> logger.warn("Unknown message type: " + messageType);
            }
        } catch (IOException e) {
            logger.error("Failed to decode plugin message", e);
        }
    }





}


