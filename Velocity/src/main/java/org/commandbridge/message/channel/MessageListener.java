package org.commandbridge.message.channel;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.commandbridge.CommandBridge;
import org.commandbridge.utilities.VerboseLogger;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class MessageListener {
    private final CommandBridge plugin;
    private final VerboseLogger logger;

    @Inject
    public MessageListener(CommandBridge plugin) {
        this.plugin = plugin;
        this.logger = plugin.getVerboseLogger();
    }

    @Subscribe
    public void onPluginMessageReceived(PluginMessageEvent event) {
        logger.info("Received plugin message on channel " + event.getIdentifier().getId());

        if (!event.getIdentifier().equals(plugin.getChannelIdentifier())) {
            logger.warn("Message received on unrecognized channel: " + event.getIdentifier().getId());
            return;
        }

        logger.info("Received valid plugin message on channel " + plugin.getChannelIdentifier().getId());

        if (event.getSource() instanceof Player player) {
            byte[] data = event.getData();

            logger.info("Message received from player: " + player.getUsername());
            decodePluginMessage(player, data);

        } else if (event.getSource() instanceof RegisteredServer server) {
            byte[] data = event.getData();

            logger.info("Message received from server: " + server.getServerInfo().getName());
            decodePluginMessage(server, data);

        } else {
            logger.warn("Received plugin message from unknown source type");
        }
    }

    private void decodePluginMessage(Object source, byte[] data) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
             DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream)) {

            String playerUUID = dataInputStream.readUTF();
            String command = dataInputStream.readUTF();

            if (source instanceof Player player) {
                logger.info("Received plugin message from player " + player.getUsername() + ": " + playerUUID + " " + command);
                executeCommand(player, playerUUID, command);
            } else if (source instanceof RegisteredServer server) {
                logger.info("Received plugin message from server " + server.getServerInfo().getName() + ": " + playerUUID + " " + command);
                executeCommand(server, playerUUID, command);
            } else {
                logger.warn("Decoded message from an unknown source");
            }

        } catch (IOException e) {
            logger.error("Failed to decode plugin message", e);
        }
    }

    private void executeCommand(Object source, String UUID, String command) {
        if (source instanceof Player player) {
            logger.info("Executing command for player: " + player.getUsername() + " Command: " + command);

        } else if (source instanceof RegisteredServer server) {
            logger.info("Executing command for server: " + server.getServerInfo().getName() + " Command: " + command);

        }
    }



}
