package org.commandbridge;

import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class MessageSender {
    private final CommandBridge plugin;
    private final VerboseLogger verboseLogger;

    public MessageSender(CommandBridge plugin) {
        this.plugin = plugin;
        this.verboseLogger = plugin.getVerboseLogger();
    }

    public void sendPluginMessage(Player player, String command) {
        try {
            verboseLogger.info("Successfully send: " + command);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeUTF(player.getUniqueId().toString());
            dataOutputStream.writeUTF(command);

            player.sendPluginMessage(plugin, "commandbridge:main", byteArrayOutputStream.toByteArray());

        } catch (Exception e) {
            verboseLogger.error("Failed to send plugin message", e);
        }
    }
}
