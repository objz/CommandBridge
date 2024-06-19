package org.commandbridge;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MessageListener {
    private final CommandBridge plugin;
    private final VerboseLogger logger;
    private final ProxyServer server;

    @Inject
    public MessageListener(ProxyServer proxyServer, CommandBridge plugin) {
        this.plugin = plugin;
        this.server = proxyServer;
        this.logger = plugin.getVerboseLogger();
    }

    @Subscribe
    public void onPluginMessageReceived(PluginMessageEvent event) {
        logger.info("Received plugin message on channel " + event.getIdentifier().getId());
       if (!event.getIdentifier().equals(plugin.getChannelIdentifier())) {
           return;
       }

       logger.info("Received plugin message on channel " + plugin.getChannelIdentifier().getId());
       if (event.getSource() instanceof Player) {
           Player player = (Player) event.getSource();
              byte[] data = event.getData();

            decodePluginMessage(player, data);

       } else {
           logger.warn("Received plugin message from non-player source");
       }


    }

    private void decodePluginMessage(Player player, byte[] data) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
             DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream)) {

            String message = dataInputStream.readUTF();
            String command = dataInputStream.readUTF();

            logger.info("Received plugin message from " + player.getUsername() + ": " + message + " " + command);

            executeCommand(player, message, command);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void executeCommand(Player player, String message, String command) {
        logger.info("Executing command: " + command);
        // Execute command here
    }


}
