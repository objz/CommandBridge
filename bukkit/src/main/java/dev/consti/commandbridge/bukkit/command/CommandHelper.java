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

public class CommandHelper {
    private final Logger logger;
    private final Main plugin;

    public CommandHelper(Logger logger, Main plugin) {
        this.logger = logger;
        this.plugin = plugin;
    }

    public int executeScriptCommands(CommandSender sender, ScriptManager.ScriptConfig script, String[] args) {
        logger.debug("Executing script commands for script: {}", script.getName());

        if (!script.shouldIgnorePermissionCheck() && !sender.hasPermission("commandbridge.command." + script.getName())) {
            logger.warn("Permission check failed for sender: {}", sender.getName());
            if (!script.shouldHidePermissionWarning()) {
                sender.sendMessage(NamedTextColor.RED + "You do not have permission to use this command.");
            }
            return 0;
        }

        for (ScriptManager.Command cmd : script.getCommands()) {
            logger.debug("Processing command: {}", cmd.getCommand());
            if (cmd.isCheckIfExecutorIsPlayer() && !(sender instanceof Player) && cmd.getTargetExecutor().equals("player")) {
                logger.warn("This command can only be used by a player.");
                return 0;
            }

            processCommand(cmd, sender, args);
        }
        logger.info("Script commands executed successfully for script: {}", script.getName());
        return 1; 
    }

    private void processCommand(ScriptManager.Command cmd, CommandSender sender, String[] args) {
        logger.debug("Starting processCommand for: {}", cmd.getCommand());

        Player player = (sender instanceof Player) ? (Player) sender : null;
        StringParser parser = StringParser.create();

        if (player != null && cmd.getTargetExecutor().equals("player")) {
            logger.debug("Adding placeholders for player: {}", player.getName());
            parser.addPlaceholder("%player%", player.getName());
            parser.addPlaceholder("%uuid%", player.getUniqueId().toString());
            parser.addPlaceholder("%world%", player.getWorld().getName());

            if (cmd.isCheckIfExecutorIsOnServer()) {
                // Implement server check logic if needed
            }
        } else if (cmd.getTargetExecutor().equals("player") && player == null) {
            logger.warn("Player is null or not executing as a player.");
            return;
        } else {
            logger.debug("Executing as console or non-player sender.");
        }

        String commandStr = parser.parsePlaceholders(cmd.getCommand(), args);
        logger.debug("Parsed command: {}", commandStr);

        if (cmd.getDelay() > 0) {
            logger.debug("Scheduling command with delay: {} seconds", cmd.getDelay());
            Bukkit.getScheduler().runTaskLater(plugin, () -> executeCommand(cmd, commandStr, args, player), cmd.getDelay() * 20L);
        } else {
            logger.debug("Executing command immediately.");
            executeCommand(cmd, commandStr, args, player);
        }
    }

    private void executeCommand(ScriptManager.Command cmd, String commandStr, String[] args, Player player) {
        logger.debug("Executing command: {}", cmd.getCommand());

        if (cmd.shouldWaitUntilPlayerIsOnline() && cmd.getTargetExecutor().equals("player") && (player == null || !player.isOnline())) {
            logger.warn("Player is not online. Retrying command: {}", cmd.getCommand());
            Bukkit.getScheduler().runTaskLater(plugin, () -> executeCommand(cmd, commandStr, args, player), 20L);
            return;
        }

        if (!cmd.getTargetServerIds().isEmpty()) {
            logger.debug("Handling server-specific execution for command: {}", cmd.getCommand());
            for (String serverId : cmd.getTargetServerIds()) {
                try {
                    logger.info("Sending command to server: {}", serverId);
                    Runtime.getInstance().getClient().sendJSON(commandStr, serverId, args, player, cmd.getTargetExecutor());
                } catch (Exception e) {
                    logger.error("Failed to send command to server {}: {}", serverId, e.getMessage());
                }
            }
        } else {
            logger.warn("Target server IDs are empty. Command: {}", cmd.getCommand());
        }
    }
}
