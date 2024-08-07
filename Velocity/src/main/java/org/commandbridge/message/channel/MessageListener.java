package org.commandbridge.message.channel;

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
    private String lastUUID;
    private String lastVersionUUID;

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
            logger.info("Message received on unrecognized channel: " + event.getIdentifier().getId());
            return;
        }

        logger.info("Received valid plugin message on channel " + event.getIdentifier().getId());
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
            String targetVelocityServer = dataInputStream.readUTF();
            String currentUUID = dataInputStream.readUTF();
            if (!targetVelocityServer.equals(plugin.getServerId())) {
                logger.warn("Received message for a different server: " + targetVelocityServer + ", current server name: " + plugin.getServerId());
                return;
            }
            logger.info("Message type: " + messageType);

            if (currentUUID.equals(lastUUID)) {
                logger.info("Received message multiply times - canceling");
                return;
            }
            lastUUID = currentUUID;

            switch (messageType) {
                case "ExecuteCommand":
                    handleExecuteCommand(dataInputStream);
                    break;
                case "SystemCommand":
                    handleSystemCommand(dataInputStream);
                    break;
                case "Version":
                    handleVersion(dataInputStream);
                    break;
                default:
                    logger.warn("Unknown message type: " + messageType);
                    break;
            }
        } catch (IOException e) {
            logger.error("Failed to decode plugin message", e);
        }
    }

    private void handleExecuteCommand(DataInputStream dataInputStream) throws IOException {
        String executor = dataInputStream.readUTF();
        logger.info("Executor type: " + executor);

        switch (executor) {
            case "player" -> {
                String playerUUID = dataInputStream.readUTF();
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
            }
            case "console" -> {
                String playerUUID = dataInputStream.readUTF();
                String command = dataInputStream.readUTF();
                logger.info("Received command for console: " + command + ", player UUID: " + playerUUID);
                proxyServer.getCommandManager().executeAsync(proxyServer.getConsoleCommandSource(), command);
                logger.info("Executed command as console: " + command);
            }
            default -> logger.warn("Invalid executor: " + executor);
        }
    }

    private void handleSystemCommand(DataInputStream dataInputStream) throws IOException {
        String command = dataInputStream.readUTF();
        logger.info("Received system command: " + command);
    }

    private void handleVersion(DataInputStream dataInputStream) throws IOException {
        String currentUUID = dataInputStream.readUTF();
        String version = dataInputStream.readUTF();

        if (currentUUID.equals(lastVersionUUID)) {
            plugin.getStartup().isSameBukkitVersion(VersionChecker.checkBukkitVersion(version, plugin.getVersion()));
        }
        lastVersionUUID = currentUUID;
    }
}
