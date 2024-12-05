package dev.consti.commandbridge.bukkit.command;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

import dev.consti.commandbridge.bukkit.Main;
import dev.consti.commandbridge.bukkit.core.Runtime;
import dev.consti.foundationlib.json.MessageParser;
import dev.consti.foundationlib.logging.Logger;

public class CommandExecutor {
    private final Main plugin;
    private final Logger logger;

    public CommandExecutor() {
        this.plugin = Main.getInstance();
        this.logger = Runtime.getInstance().getLogger();
    }

    public void dispatchCommand(String message) {
        logger.debug("Received message: {}", message);

        MessageParser parser = new MessageParser(message);

        // Validate client
        String serverId = Runtime.getInstance().getConfig().getKey("config.yml", "client-id");
        if (!parser.getBodyValueAsString("client").equals(serverId)) {
            logger.debug("Message not intended for this client. Server ID: {}", serverId);
            return;
        }

        String command = parser.getBodyValueAsString("command");
        String target = parser.getBodyValueAsString("target");

        logger.info("Dispatching command: '{}' for target: {}", command, target);

        switch (target.toLowerCase()) {
            case "console":
                executeConsoleCommand(command);
                break;
            case "player":
                executePlayerCommand(parser, command);
                break;
            default:
                logger.warn("Invalid target value: '{}'", target);
        }
    }

    private void executeConsoleCommand(String command) {
        logger.debug("Executing command on console: {}", command);
        if (isCommandValid(command)) {
            CommandSender console = Bukkit.getConsoleSender();
            boolean success = Bukkit.dispatchCommand(console, command);
            if (success) {
                logger.info("Successfully executed command on console: {}", command);
            } else {
                logger.error("Failed to execute command on console: {}", command);
            }
        } else {
            logger.warn("Invalid command: '{}'", command);
        }
    }

    private void executePlayerCommand(MessageParser parser, String command) {
        String uuidStr = parser.getBodyValueAsString("uuid");
        String name = parser.getBodyValueAsString("name");

        logger.debug("Looking for player UUID: {}, Name: {}", uuidStr, name);

        try {
            UUID playerUuid = UUID.fromString(uuidStr);
            Player player = Bukkit.getPlayer(playerUuid);

            if (player != null && player.isOnline()) {
                if (isCommandValid(command)) {
                    logger.info("Executing command for player {}: {}", name, command);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        boolean success = Bukkit.dispatchCommand(player, command);
                        if (success) {
                            logger.info("Successfully executed command for player {}: {}", name, command);
                        } else {
                            logger.warn("Failed to execute command for player {}: {}", name, command);
                        }
                    });
                } else {
                    logger.warn("Invalid command: '{}' for player {}", command, name);
                    player.sendMessage("§cThe command '" + command + "' is invalid.");
                }
            } else {
                logger.warn("Player not found or offline. UUID: {}, Name: {}", uuidStr, name);
            }
        } catch (IllegalArgumentException e) {
            logger.error("Invalid UUID format: {}", uuidStr, e);
        }
    }

    private boolean isCommandValid(String command) {
        String baseCommand = command.split(" ")[0]; // Extract the base command
        PluginCommand pluginCommand = Bukkit.getPluginCommand(baseCommand);
        return pluginCommand != null;
    }
}

