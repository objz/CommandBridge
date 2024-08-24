package org.commandbridge.handler;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.commandbridge.CommandBridge;
import org.commandbridge.message.channel.MessageSender;
import org.commandbridge.utilities.VerboseLogger;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandExecutor {
    private static final int TIMEOUT_LIMIT = 20; // seconds
    private final ProxyServer server;
    private final VerboseLogger verboseLogger;
    private final CommandBridge plugin;
    private final MessageSender bridge;
    private boolean timeoutMessageSent = false;
    private boolean timeoutServer = false;

    public CommandExecutor(ProxyServer server, CommandBridge plugin) {
        this.server = server;
        this.plugin = plugin;
        this.bridge = new MessageSender(server, plugin);
        this.verboseLogger = plugin.getVerboseLogger();
    }

    public void resetState() {
        timeoutMessageSent = false;
        timeoutServer = false;
    }

    public void executeCommand(String command, List<String> targetServerIds, String targetExecutor, boolean waitForOnline, Player playerMessage, AtomicInteger timeElapsed, String playerUUID, boolean disablePlayerOnline) {
        for (String targetServerId : targetServerIds) {
            server.getServer(targetServerId).ifPresentOrElse(serverConnection -> {
                if (waitForOnline) {
                    waitForPlayerAndExecute(command, targetServerId, targetExecutor, playerMessage, timeElapsed, playerUUID, disablePlayerOnline);
                } else {
                    executeOrSendMessage(command, targetServerId, targetExecutor, playerMessage, playerUUID, disablePlayerOnline);
                }
            }, () -> verboseLogger.warn("Target server not found: " + targetServerId));
        }
    }

    private void waitForPlayerAndExecute(String command, String targetServerId, String targetExecutor, Player playerMessage, AtomicInteger timeElapsed, String playerUUID, boolean disablePlayerOnline) {
        server.getServer(targetServerId).ifPresent(serverConnection -> serverConnection.getPlayersConnected().stream()
                .filter(player -> player.getUniqueId().toString().equals(playerUUID))
                .findFirst()
                .ifPresentOrElse(player -> executeCommand(command, targetServerId, targetExecutor),
                        () -> {
                            if (timeElapsed.getAndIncrement() < TIMEOUT_LIMIT) {
                                server.getScheduler().buildTask(plugin, () -> executeCommand(command, List.of(targetServerId), targetExecutor, true, playerMessage, timeElapsed, playerUUID, disablePlayerOnline))
                                        .delay(1, TimeUnit.SECONDS)
                                        .schedule();
                                verboseLogger.info("Waiting for player to be online on server " + targetServerId + ": " + command);
                            } else {
                                handleTimeout(playerMessage, targetServerId, command);
                            }
                        }));
    }

    private void executeCommand(String command, String targetServerId, String targetExecutor) {
        bridge.sendCommandToBukkit(command, targetServerId, targetExecutor);
        verboseLogger.info("Executing command on server " + targetServerId + ": " + command);
    }

    private void handleTimeout(Player playerMessage, String targetServerId, String command) {
        if (!timeoutServer) {
            playerMessage.sendMessage(Component.text("Timeout reached. Player not online within 20 seconds on server " + targetServerId, NamedTextColor.RED));
            verboseLogger.warn("Timeout reached. Player not online on server " + targetServerId + ": " + command);
            timeoutServer = true;
        }
    }

    private void executeOrSendMessage(String command, String targetServerId, String targetExecutor, Player playerMessage, String playerUUID, boolean disablePlayerOnline) {
        server.getServer(targetServerId).ifPresent(serverConnection -> {
            if (disablePlayerOnline) {
                executeCommand(command, targetServerId, targetExecutor);
            } else {
                serverConnection.getPlayersConnected().stream()
                        .filter(player -> player.getUniqueId().toString().equals(playerUUID))
                        .findFirst()
                        .ifPresentOrElse(player -> executeCommand(command, targetServerId, targetExecutor),
                                () -> sendPlayerNotOnlineMessage(playerMessage, targetServerId, command));
            }
        });
    }

    private void sendPlayerNotOnlineMessage(Player playerMessage, String targetServerId, String command) {
        if (!timeoutMessageSent) {
            playerMessage.sendMessage(Component.text("You must be on the server " + targetServerId + " to use this command.", NamedTextColor.RED));
            verboseLogger.warn("Player is not online on server " + targetServerId + ": " + command);
            timeoutMessageSent = true;
        }
    }
}
