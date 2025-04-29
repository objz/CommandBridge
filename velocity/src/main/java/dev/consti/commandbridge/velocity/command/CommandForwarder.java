package dev.consti.commandbridge.velocity.command;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import dev.consti.commandbridge.velocity.Main;
import dev.consti.commandbridge.velocity.core.Runtime;
import dev.consti.commandbridge.velocity.util.ProxyUtils;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.foundationlib.utils.ScriptManager;
import dev.consti.foundationlib.utils.StringParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.william278.papiproxybridge.api.PlaceholderAPI;

public class CommandForwarder {
    private final Logger logger;
    private final ProxyServer proxy;
    private final Main plugin;

    public CommandForwarder(Logger logger, Main plugin) {
        this.logger = logger;
        this.proxy = ProxyUtils.getProxyServer();
        this.plugin = plugin;
    }

    public int executeScriptCommands(CommandSource source, ScriptManager.ScriptConfig script, String[] args) {
        logger.debug("Executing script commands for script: {}", script.getName());

        if (isPermissionDenied(source, script)) {
            return 0;
        }

        for (ScriptManager.Command cmd : script.getCommands()) {
            logger.debug("Processing command: {}", cmd.getCommand());

            switch (cmd.getTargetExecutor().toLowerCase()) {
                case "player" -> handlePlayerExecutor(cmd, source, args);
                case "console" -> handleConsoleExecutor(cmd, source, args);
                default -> logger.warn("Unknown target executor for command: {}", cmd.getCommand());
            }
        }

        logger.info("Script commands executed successfully for command: {}", script.getName());
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private boolean isPermissionDenied(CommandSource source, ScriptManager.ScriptConfig script) {
        if (!script.shouldIgnorePermissionCheck()
                && !source.hasPermission("commandbridge.command." + script.getName())) {
            logger.warn("Sender '{}' has no permission to use this command", source);
            if (!script.shouldHidePermissionWarning()) {
                source.sendMessage(
                        Component.text("You do not have permission to use this command", NamedTextColor.RED));
            }
            return true;
        }
        return false;
    }

    private void handlePlayerExecutor(ScriptManager.Command cmd, CommandSource source, String[] args) {
        if (cmd.isCheckIfExecutorIsPlayer() && !(source instanceof Player)) {
            logger.warn("This command requires a player as executor, but source is not a player");
            source.sendMessage(
                    Component.text("This command requires a player as executor, but source is not a player object",
                            NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;

        // Check if the player is on the required server if needed
        if (cmd.isCheckIfExecutorIsOnServer() && !isPlayerOnTargetServer(player, cmd)) {
            logger.warn("Player '{}' is not on the required server for this command.", player.getUsername());
            source.sendMessage(Component.text("Player " + player.getUsername() + " is not on the required server",
                    NamedTextColor.YELLOW));
            return;
        }

        parseCommand(cmd, args, player).thenAccept(parsedCommand -> {
            if (parsedCommand == null)
                return;

            if (cmd.getDelay() > 0) {
                scheduleCommand(cmd, parsedCommand, args, player, 0);
            } else {
                sendCommand(cmd, parsedCommand, args, player, 0);
            }
        });

    }

    private void handleConsoleExecutor(ScriptManager.Command cmd, CommandSource source, String[] args) {

        parseCommand(cmd, args, null).thenAccept(parsedCommand -> {
            if (parsedCommand == null)
                return;

            if (cmd.getDelay() > 0) {
                scheduleCommand(cmd, parsedCommand, args, null, 0);
            } else {
                sendCommand(cmd, parsedCommand, args, null, 0);
            }
        });

    }

    private boolean isPlayerOnTargetServer(Player player, ScriptManager.Command cmd) {
        return player.getCurrentServer()
                .map(serverConn -> cmd.getTargetClientIds().contains(serverConn.getServerInfo().getName()))
                .orElse(false);
    }

    private CompletableFuture<String> parseCommand(ScriptManager.Command cmd, String[] args, Player player) {
        StringParser parser = StringParser.create();

        if (player != null && cmd.getTargetExecutor().equals("player")) {
            addPlayerPlaceholders(parser, player);
        } else {
            return CompletableFuture.completedFuture(parser.parsePlaceholders(cmd.getCommand(), args));
        }

        try {
            String parsedCommand = parser.parsePlaceholders(cmd.getCommand(), args);

            if (Runtime.getInstance().getStartup().isPlaceholderAPI()) {
                return PlaceholderAPI.createInstance()
                        .formatPlaceholders(parsedCommand, player.getUniqueId())
                        .exceptionally(e -> {
                            logger.error("PlaceholderAPI error: {}", logger.getDebug() ? e : e.getMessage());
                            return parsedCommand; // Fallback
                        });
            }

            return CompletableFuture.completedFuture(parsedCommand);
        } catch (Exception e) {
            logger.error("Error occurred while parsing command: {}", logger.getDebug() ? e : e.getMessage());
            if (player != null) {
                player.sendMessage(Component.text("Error occurred while parsing command").color(NamedTextColor.RED));
            }
            for (String conn : cmd.getTargetClientIds()) {
                Runtime.getInstance().getServer().sendError(
                        Runtime.getInstance().getServer().getWebSocket(conn),
                        "Error occurred while parsing commands");
            }

            return CompletableFuture.completedFuture(null);
        }
    }

    private void addPlayerPlaceholders(StringParser parser, Player player) {
        logger.debug("Adding placeholders for player: {}", player.getUsername());
        parser.addPlaceholder("%cb_player%", player.getUsername());
        parser.addPlaceholder("%cb_uuid%", player.getUniqueId().toString());
        parser.addPlaceholder("%cb_server%", player.getCurrentServer()
                .map(srv -> srv.getServerInfo().getName())
                .orElse("defaultServerName"));
    }

    private void scheduleCommand(ScriptManager.Command cmd, String command, String[] args, Player player,
            int retryCount) {
        logger.debug("Scheduling command '{}' with delay: {} seconds", cmd.getCommand(), cmd.getDelay());
        proxy.getScheduler().buildTask(plugin, () -> sendCommand(cmd, command, args, player, retryCount))
                .delay(cmd.getDelay(), TimeUnit.SECONDS)
                .schedule();
    }

    private void sendCommand(ScriptManager.Command cmd, String command, String[] args, Player player, int retryCount) {
        logger.debug("Executing command: {} with retryCount: {}", cmd.getCommand(), retryCount);

        // Prevent infinite loops
        if (retryCount >= 30) {
            logger.warn("Max retries reached for command: {}", cmd.getCommand());
            if (player != null) {
                player.sendMessage(Component.text("Max retries reached", NamedTextColor.YELLOW));
            }
            return;
        }

        // If we need the player to be online, check it here
        if (cmd.shouldWaitUntilPlayerIsOnline() && "player".equalsIgnoreCase(cmd.getTargetExecutor())) {
            if (player == null || !player.isActive()) {
                logger.warn("Player is not online. Retrying command: {}", cmd.getCommand());
                player.sendMessage(Component.text("Player is not online. Retrying command", NamedTextColor.YELLOW));
                proxy.getScheduler().buildTask(plugin, () -> sendCommand(cmd, command, args, player, retryCount + 1))
                        .delay(1, TimeUnit.SECONDS)
                        .schedule();
                return;
            }
        }

        List<String> targetClients = cmd.getTargetClientIds();
        if (targetClients.isEmpty()) {
            logger.warn("No target clients defined for command: {}", cmd.getCommand());
            if (player != null) {
                player.sendMessage(
                        Component.text("No target clients are defined for this command", NamedTextColor.RED));
            }
            return;
        }

        for (String clientId : targetClients) {
            if (Runtime.getInstance().getServer().isServerConnected(clientId)) {
                logger.info("Sending command to client '{}' as {}", clientId, player == null ? "console" : "player");
                Runtime.getInstance().getServer().sendCommand(command, clientId, cmd.getTargetExecutor(), player);
            } else {
                logger.warn("Client '{}' not found", clientId);
                if (player != null) {
                    player.sendMessage(Component.text("Client '" + clientId + "' not found", NamedTextColor.RED));
                }
            }
        }
    }
}
