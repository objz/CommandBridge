package dev.consti.commandbridge.velocity.command;

import java.util.Optional;
import java.util.UUID;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import dev.consti.commandbridge.velocity.Main;
import dev.consti.commandbridge.velocity.core.Runtime;
import dev.consti.commandbridge.velocity.utils.ProxyUtils;
import dev.consti.foundationlib.json.MessageParser;
import dev.consti.foundationlib.logging.Logger;

public class CommandExecutor {
    private final ProxyServer proxy;
    private final Main plugin;
    private final Logger logger;

    public CommandExecutor() {
        this.plugin = Main.getInstance();
        this.proxy = ProxyUtils.getProxyServer();
        this.logger = Runtime.getInstance().getLogger();
    }

    public void dispatchCommand(String message) {
        logger.debug("Received message: {}", message);

        MessageParser parser = new MessageParser(message);

        // Validate server
        String serverId = Runtime.getInstance().getConfig().getKey("config.yml", "server-id");
        if (!parser.getBodyValueAsString("server").equals(serverId)) {
            logger.debug("Message not intended for this server. Server ID: {}", serverId);
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
                executePlayerCommand(parser);
                break;
            default:
                logger.warn("Invalid target value: '{}'", target);
        }
    }

    private void executeConsoleCommand(String command) {
        logger.debug("Executing command on console: {}", command);
        proxy.getCommandManager()
                .executeAsync(proxy.getConsoleCommandSource(), command)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        logger.info("Successfully executed command on console: {}", command);
                    } else {
                        logger.error("Error executing command on console: {}", command, throwable);
                    }
                });
    }

    private void executePlayerCommand(MessageParser parser) {
        String uuidStr = parser.getBodyValueAsString("uuid");
        String name = parser.getBodyValueAsString("name");
        String command = parser.getBodyValueAsString("command");

        logger.debug("Looking for player UUID: {}, Name: {}", uuidStr, name);

        try {
            UUID playerUuid = UUID.fromString(uuidStr);
            Optional<Player> playerOpt = proxy.getPlayer(playerUuid);

            if (playerOpt.isPresent()) {
                Player player = playerOpt.get();
                logger.info("Executing command for player {}: {}", name, command);
                proxy.getCommandManager()
                        .executeAsync(player, command)
                        .whenComplete((result, throwable) -> {
                            if (throwable == null) {
                                logger.info("Successfully executed command for player {}: {}", name, command);
                            } else {
                                logger.error("Error executing command for player {}: {}", name, command, throwable);
                            }
                        });
            } else {
                logger.warn("Player not found. UUID: {}, Name: {}", uuidStr, name);
            }
        } catch (IllegalArgumentException e) {
            logger.error("Invalid UUID format: {}", uuidStr, e);
        }
    }
}
