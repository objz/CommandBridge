package dev.consti.commandbridge.bukkit.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.consti.commandbridge.bukkit.Main;
import dev.consti.commandbridge.bukkit.core.Runtime;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.foundationlib.utils.ScriptManager;
import dev.consti.foundationlib.utils.StringParser;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public class CommandHelper {
    private final Logger logger;
    private final Main plugin;

    public CommandHelper(Logger logger, Main plugin) {
        this.logger = logger;
        this.plugin = plugin;
    }

    public int executeScriptCommands(CommandSender sender, ScriptManager.ScriptConfig script, String[] args) {
        if (isPermissionDenied(sender, script)) return 0;

        for (ScriptManager.Command cmd : script.getCommands()) {
            logger.debug("Processing command: {}", cmd.getCommand());
            if (isInvalidPlayerExecutor(sender, cmd)) return 0;

            processCommand(cmd, sender, args);
        }
        return 1;
    }

    private boolean isPermissionDenied(CommandSender sender, ScriptManager.ScriptConfig script) {
        if (!script.shouldIgnorePermissionCheck() && !sender.hasPermission("commandbridge.command." + script.getName())) {
            logger.warn("Permission check failed for sender: {}", sender.getName());
            if (!script.shouldHidePermissionWarning()) {
                sender.sendMessage(NamedTextColor.RED + "You do not have permission to use this command.");
            }
            return true;
        }
        return false;
    }

    private boolean isInvalidPlayerExecutor(CommandSender sender, ScriptManager.Command cmd) {
        if (cmd.isCheckIfExecutorIsPlayer() && !(sender instanceof Player) && "player".equals(cmd.getTargetExecutor())) {
            logger.warn("This command can only be used by a player.");
            return true;
        }
        return false;
    }

    private void processCommand(ScriptManager.Command cmd, CommandSender sender, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        String commandStr = prepareCommandString(cmd, args, player);

        if (commandStr == null) return;

        if (cmd.getDelay() > 0) {
            scheduleCommandWithDelay(cmd, commandStr, args, player);
        } else {
            executeCommand(cmd, commandStr, args, player);
        }
    }

    private String prepareCommandString(ScriptManager.Command cmd, String[] args, Player player) {
        StringParser parser = StringParser.create();
        if (player != null && "player".equals(cmd.getTargetExecutor())) {
            addPlayerPlaceholders(parser, player);
        } else if ("player".equals(cmd.getTargetExecutor()) && player == null) {
            logger.warn("Player is null or not executing as a player: {}", cmd.getCommand());
            return null;
        }
        return parser.parsePlaceholders(cmd.getCommand(), args);
    }

    private void addPlayerPlaceholders(StringParser parser, Player player) {
        logger.debug("Adding placeholders for player: {}", player.getName());
        parser.addPlaceholder("%player%", player.getName());
        parser.addPlaceholder("%uuid%", player.getUniqueId().toString());
        parser.addPlaceholder("%world%", player.getWorld().getName());
    }

    private void scheduleCommandWithDelay(ScriptManager.Command cmd, String commandStr, String[] args, Player player) {
        logger.debug("Scheduling command '{}' with delay: {} seconds", cmd.getCommand(), cmd.getDelay());
        Bukkit.getScheduler().runTaskLater(plugin, () -> executeCommand(cmd, commandStr, args, player), cmd.getDelay() * 20L);
    }

    private void executeCommand(ScriptManager.Command cmd, String commandStr, String[] args, Player player) {
        if (shouldRetryForOfflinePlayer(cmd, player)) return;

        List<String> targetServers = cmd.getTargetServerIds();
        if (targetServers.isEmpty()) {
            logger.warn("Target server IDs are empty. Command: {}", cmd.getCommand());
            return;
        }

        sendCommandToServers(cmd, commandStr, args, player, targetServers);
    }

    private boolean shouldRetryForOfflinePlayer(ScriptManager.Command cmd, Player player) {
        if (cmd.shouldWaitUntilPlayerIsOnline() && "player".equals(cmd.getTargetExecutor()) && (player == null || !player.isOnline())) {
            logger.warn("Player is not online. Retrying command: {}", cmd.getCommand());
            Bukkit.getScheduler().runTaskLater(plugin, () -> executeCommand(cmd, cmd.getCommand(), null, player), 20L);
            return true;
        }
        return false;
    }

    private void sendCommandToServers(ScriptManager.Command cmd, String commandStr, String[] args, Player player, List<String> targetServers) {
        for (String serverId : targetServers) {
            try {
                logger.info("Sending command to server: {}", serverId);
                Runtime.getInstance().getClient().sendCommand(commandStr, serverId, args, player, cmd.getTargetExecutor());
            } catch (Exception e) {
                logger.error("Failed to send command to server {}: {}", serverId, e.getMessage());
            }
        }
    }
}
