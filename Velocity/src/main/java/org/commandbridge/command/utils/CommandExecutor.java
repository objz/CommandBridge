package org.commandbridge.command.utils;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.commandbridge.CommandBridge;
import org.commandbridge.message.channel.Bridge;
import org.commandbridge.utilities.VerboseLogger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandExecutor {
    private final ProxyServer server;
    private final VerboseLogger verboseLogger;
    private final CommandBridge plugin;
    private final Bridge bridge;
    private boolean timeoutMessageSent = false;
    private boolean timeoutServer = false;

    public CommandExecutor(ProxyServer server, CommandBridge plugin) {
        this.server = server;
        this.plugin = plugin;
        this.bridge = new Bridge(server, plugin);
        this.verboseLogger = plugin.getVerboseLogger();
    }

    public void resetState() {
        timeoutMessageSent = false;
        timeoutServer = false;
    }


    public void executeCommand(String command, String targetServerId, String targetExecutor, boolean waitForOnline, Player playerMessage, AtomicInteger timeElapsed, String playerUUID, boolean disable_player_online) {
        server.getServer(targetServerId).ifPresent(serverConnection -> {
            if (waitForOnline) {
                serverConnection.getPlayersConnected().stream()
                        .filter(player -> player.getUniqueId().toString().equals(playerUUID)) // Der Spielername ist das erste Argument im Befehl.
                        .findFirst()
                        .ifPresentOrElse(player -> {
                            bridge.sendCommandToBukkit(command, targetServerId, targetExecutor);
                            verboseLogger.info("Executing command on server " + targetServerId + ": " + command);
                        }, () -> {
                            if (timeElapsed.getAndIncrement() < 20) {
                                server.getScheduler().buildTask(plugin, () -> executeCommand(command, targetServerId, targetExecutor, true, playerMessage, timeElapsed, playerUUID, disable_player_online))
                                        .delay(1, TimeUnit.SECONDS)
                                        .schedule();
                                verboseLogger.info("Waiting for player to be online on server " + targetServerId + ": " + command);
                            } else {
                                if (!timeoutServer) {
                                    playerMessage.sendMessage(Component.text("Timeout reached. Player not online within 20 seconds on server " + targetServerId, net.kyori.adventure.text.format.NamedTextColor.RED));
                                    verboseLogger.warn("Timeout reached. Player not online on server " + targetServerId + ": " + command);
                                    timeoutServer = true;
                                }
                            }
                        });
            } else {
                checkAndExecute(command, targetServerId, targetExecutor, playerMessage, playerUUID, disable_player_online);
            }
        });
    }

    private void checkAndExecute(String command, String targetServerId, String targetExecutor, Player playerMessage, String playerUUID, boolean disable_player_online) {
        server.getServer(targetServerId).ifPresent(serverConnection -> {
            verboseLogger.info("Player UUID: " + playerUUID);


            if (disable_player_online) {
                bridge.sendCommandToBukkit(command, targetServerId, targetExecutor);
                verboseLogger.info("Executing command: " + command);

            } else {
                serverConnection.getPlayersConnected().stream()
                        .filter(player -> player.getUniqueId().toString().equals(playerUUID))
                        .findFirst()
                        .ifPresentOrElse(player -> {
                            bridge.sendCommandToBukkit(command, targetServerId, targetExecutor);
                            verboseLogger.info("Executing command: " + command);
                        }, () -> {
                            if (!timeoutMessageSent) {
                                playerMessage.sendMessage(Component.text("You must be on the server " + targetServerId + " to use this command.", net.kyori.adventure.text.format.NamedTextColor.RED));
                                verboseLogger.warn("Player is not online on server " + targetServerId + ": " + command);
                                timeoutMessageSent = true;
                            }
                        });
            }
        });
    }
}
