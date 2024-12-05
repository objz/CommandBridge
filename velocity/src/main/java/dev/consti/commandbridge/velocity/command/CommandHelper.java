package dev.consti.commandbridge.velocity.command;

import java.util.concurrent.TimeUnit;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import dev.consti.commandbridge.velocity.Main;
import dev.consti.commandbridge.velocity.core.Runtime;
import dev.consti.commandbridge.velocity.utils.ProxyUtils;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.foundationlib.utils.ScriptManager;
import dev.consti.foundationlib.utils.StringParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CommandHelper {
    private final Logger logger;
    private final ProxyServer proxy;
    private final Main plugin;

    public CommandHelper(Logger logger, Main plugin) {
        this.logger = logger;
        this.proxy = ProxyUtils.getProxyServer();
        this.plugin = plugin;
    }

    public int executeScriptCommands(CommandSource source, ScriptManager.ScriptConfig script, String[] args) {
        logger.debug("Executing script commands for script: {}", script.getName());

        if (!script.shouldIgnorePermissionCheck() && !source.hasPermission("commandbridge.command." + script.getName())) {
            logger.warn("Permission check failed for source: {}", source);
            if (!script.shouldHidePermissionWarning()) {
                source.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            }
            return 0;
        }

        for (ScriptManager.Command cmd : script.getCommands()) {
            logger.debug("Processing command: {}", cmd.getCommand());
            if (cmd.isCheckIfExecutorIsPlayer() && !(source instanceof Player) && cmd.getTargetExecutor().equals("player")) {
                logger.warn("This command can only be used by a player.");
                return 0;
            }

            processCommand(cmd, source, args);
        }
        logger.info("Script commands executed successfully for script: {}", script.getName());
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private void processCommand(ScriptManager.Command cmd, CommandSource source, String[] args) {
        logger.debug("Starting processCommand for: {}", cmd.getCommand());

        Player player = (source instanceof Player) ? (Player) source : null;
        StringParser parser = StringParser.create();

        if (player != null && cmd.getTargetExecutor().equals("player")) {
            logger.debug("Adding placeholders for player: {}", player.getUsername());
            parser.addPlaceholder("%player%", player.getUsername());
            parser.addPlaceholder("%uuid%", player.getUniqueId().toString());
            parser.addPlaceholder("%server%", player.getCurrentServer()
                    .map(serverConnection -> serverConnection.getServerInfo().getName())
                    .orElse("defaultServerName"));

            if (cmd.isCheckIfExecutorIsOnServer() && player.getCurrentServer().isPresent()) {
                String currentServer = player.getCurrentServer().get().getServerInfo().getName();
                if (!cmd.getTargetServerIds().contains(currentServer)) {
                    logger.warn("Player {} is not on the required server for this command.", player.getUsername());
                    return;
                }
            }
        } else if (cmd.getTargetExecutor().equals("player") && player == null) {
            logger.warn("Player is null or not executing as a player.");
            return;
        } else {
            logger.debug("Executing as console.");
        }

        String commandStr = parser.parsePlaceholders(cmd.getCommand(), args);
        logger.debug("Parsed command: {}", commandStr);

        if (cmd.getDelay() > 0) {
            logger.debug("Scheduling command with delay: {} seconds", cmd.getDelay());
            proxy.getScheduler().buildTask(plugin, () -> executeCommand(cmd, commandStr, args, player))
                    .delay(cmd.getDelay(), TimeUnit.SECONDS)
                    .schedule();
        } else {
            logger.debug("Executing command immediately.");
            executeCommand(cmd, commandStr, args, player);
        }
    }

    private void executeCommand(ScriptManager.Command cmd, String commandStr, String[] args, Player player, int retryCount) {
        logger.debug("Executing command: {} with retryCount: {}", cmd.getCommand(), retryCount);

        if (retryCount >= 30) {
            logger.warn("Max retries reached for command: {}", cmd.getCommand());
            return;
        }

        if (cmd.shouldWaitUntilPlayerIsOnline() && cmd.getTargetExecutor().equals("player") && (player == null || !player.isActive())) {
            logger.warn("Player is not online. Retrying command: {}", cmd.getCommand());
            proxy.getScheduler().buildTask(plugin, () -> executeCommand(cmd, commandStr, args, player, retryCount + 1))
                    .delay(1, TimeUnit.SECONDS)
                    .schedule();
            return;
        }

        if (!cmd.getTargetServerIds().isEmpty()) {
            logger.debug("Handling server-specific execution for command: {}", cmd.getCommand());
            for (String serverId : cmd.getTargetServerIds()) {
                if (Runtime.getInstance().getServer().isServerConnected(serverId)) {
                    Runtime.getInstance().getServer().sendJSON(commandStr, serverId, args, player, Boolean.parseBoolean(cmd.getTargetExecutor()));
                } else {
                    logger.warn("Server {} not found", serverId);
                }
            }
        } else {
            logger.warn("Target server ids are empty. Command: {}", cmd.getCommand());
        }
    }

    private void executeCommand(ScriptManager.Command cmd, String commandStr, String[] args, Player player) {
        logger.debug("Initial execution for command: {}", cmd.getCommand());
        executeCommand(cmd, commandStr, args, player, 0);
    }
}
