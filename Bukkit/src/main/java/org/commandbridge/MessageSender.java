package org.commandbridge;

import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class MessageSender {
    private final CommandBridge plugin;

    public MessageSender(CommandBridge plugin) {
        this.plugin = plugin;
    }

    public void sendPluginMessage(String command) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeUTF(command);

        } catch (Exception e) {
            plugin.getVerboseLogger().error("Failed to send plugin message", e);
        }
    }
}
