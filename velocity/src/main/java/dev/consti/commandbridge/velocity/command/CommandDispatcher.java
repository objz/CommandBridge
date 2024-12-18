package dev.consti.commandbridge.velocity.command;

import java.util.Optional;
import java.util.UUID;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import dev.consti.commandbridge.velocity.core.Runtime;
import dev.consti.commandbridge.velocity.util.ProxyUtils;
import dev.consti.foundationlib.json.MessageParser;
import dev.consti.foundationlib.logging.Logger;

public class CommandDispatcher {
    private final ProxyServer proxy;
    private final Logger logger;

    public CommandDispatcher() {
        this.proxy = ProxyUtils.getProxyServer();
        this.logger = Runtime.getInstance().getLogger();
    }

    public void dispatchCommand(String message) {
        MessageParser parser = new MessageParser(message);
        String command = parser.getBodyValueAsString("command");
        String target = parser.getBodyValueAsString("target");
        logger.info("Dispatching command: '{}' for target: {}", command, target);

        switch (target.toLowerCase()) {
            case "console" -> executeConsoleCommand(command);
            case "player" -> executePlayerCommand(parser, command);
            default -> logger.warn("Invalid target: {}", target);
        }
    }

    private void executeConsoleCommand(String command) {
        logger.debug("Executing command '{}' as console", command);
        proxy.getCommandManager()
                .executeAsync(proxy.getConsoleCommandSource(), command)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("Error executing console command: {}", throwable);
                    }
                });
    }

    private void executePlayerCommand(MessageParser parser, String command) {
        logger.debug("Executing command '{}' as player", command);
        String uuidStr = parser.getBodyValueAsString("uuid");
        String name = parser.getBodyValueAsString("name");

        try {
            UUID playerUuid = UUID.fromString(uuidStr);
            Optional<Player> playerOptional = proxy.getPlayer(playerUuid);

            if (playerOptional.isPresent()) {
                Player player = playerOptional.get();
                proxy.getCommandManager()
                        .executeAsync(player, command)
                        .whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                logger.error("Error executing console command: {}", throwable);
                            }
                        });
            } else {
                logger.warn("Player '{}' not found or offline", name);
            }
        } catch (Exception e) {
            logger.error("Error while processing player: {}",
                    logger.getDebug() ? e : e.getMessage()
            );
        }
    }
}
