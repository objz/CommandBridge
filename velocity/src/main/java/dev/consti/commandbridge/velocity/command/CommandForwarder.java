package dev.consti.commandbridge.velocity.command;

import java.util.List;
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

        logger.info("Script commands executed successfully for script: {}", script.getName());
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private boolean isPermissionDenied(CommandSource source, ScriptManager.ScriptConfig script) {
        if (!script.shouldIgnorePermissionCheck() && !source.hasPermission("commandbridge.command." + script.getName())) {
            logger.warn("Permission check failed for source: {}", source);
            if (!script.shouldHidePermissionWarning()) {
                source.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            }
            return true;
        }
        return false;
    }

    private void handlePlayerExecutor(ScriptManager.Command cmd, CommandSource source, String[] args) {
        if (cmd.isCheckIfExecutorIsPlayer() && !(source instanceof Player)) {
            logger.warn("This command requires a player as executor, but source is not a player.");
            return;
        }

        Player player = (Player) source;

        // Check if the player is on the required server if needed
        if (cmd.isCheckIfExecutorIsOnServer() && !isPlayerOnTargetServer(player, cmd)) {
            logger.warn("Player {} is not on the required server for this command.", player.getUsername());
            return;
        }

        String parsedCommand = parseCommand(cmd, args, player);
        if (parsedCommand == null) return;

        if (cmd.getDelay() > 0) {
            scheduleCommand(cmd, parsedCommand, args, player, 0);
        } else {
            sendCommand(cmd, parsedCommand, args, player, 0);
        }
    }

    private void handleConsoleExecutor(ScriptManager.Command cmd, CommandSource source, String[] args) {
        // No player placeholders needed
        String parsedCommand = parseCommand(cmd, args, null);
        if (parsedCommand == null) return;

        if (cmd.getDelay() > 0) {
            scheduleCommand(cmd, parsedCommand, args, null, 0);
        } else {
            sendCommand(cmd, parsedCommand, args, null, 0);
        }
    }

    private boolean isPlayerOnTargetServer(Player player, ScriptManager.Command cmd) {
        return player.getCurrentServer()
                .map(serverConn -> cmd.getTargetClientIds().contains(serverConn.getServerInfo().getName()))
                .orElse(false);
    }

    private String parseCommand(ScriptManager.Command cmd, String[] args, Player player) {
        StringParser parser = StringParser.create();

        if (player != null && cmd.getTargetExecutor().equalsIgnoreCase("player")) {
            addPlayerPlaceholders(parser, player);
        }

        return parser.parsePlaceholders(cmd.getCommand(), args);
    }

    private void addPlayerPlaceholders(StringParser parser, Player player) {
        logger.debug("Adding placeholders for player: {}", player.getUsername());
        parser.addPlaceholder("%player%", player.getUsername());
        parser.addPlaceholder("%uuid%", player.getUniqueId().toString());
        parser.addPlaceholder("%server%", player.getCurrentServer()
                .map(srv -> srv.getServerInfo().getName())
                .orElse("defaultServerName"));
    }

    private void scheduleCommand(ScriptManager.Command cmd, String command, String[] args, Player player, int retryCount) {
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
            return;
        }

        // If we need the player to be online, check it here
        if (cmd.shouldWaitUntilPlayerIsOnline() && "player".equalsIgnoreCase(cmd.getTargetExecutor())) {
            if (player == null || !player.isActive()) {
                logger.warn("Player is not online. Retrying command: {}", cmd.getCommand());
                proxy.getScheduler().buildTask(plugin, () -> sendCommand(cmd, command, args, player, retryCount + 1))
                        .delay(1, TimeUnit.SECONDS)
                        .schedule();
                return;
            }
        }

        List<String> targetClients = cmd.getTargetClientIds();
        if (targetClients.isEmpty()) {
            logger.warn("No target clients defined for command: {}", cmd.getCommand());
            return;
        }

        for (String clientId : targetClients) {
            if (Runtime.getInstance().getServer().isServerConnected(clientId)) {
                logger.info("Sending command to client '{}' as {}", clientId, player == null ? "console" : "player");
                Runtime.getInstance().getServer().sendCommand(command, clientId, cmd.getTargetExecutor(), player);
            } else {
                logger.warn("Client '{}' not found", clientId);
            }
        }
    }
}

