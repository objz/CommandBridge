package dev.objz.commandbridge.paper.command;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.objz.commandbridge.paper.Main;
import dev.objz.commandbridge.paper.core.Runtime;
import dev.objz.commandbridge.paper.utils.SchedulerAdapter;
import dev.objz.commandbridge.core.Logger;
import dev.objz.commandbridge.core.utils.ScriptManager;
import dev.objz.commandbridge.core.utils.StringParser;
import me.clip.placeholderapi.PlaceholderAPI;

public class CommandForwarder {
    private final Logger logger;
    private final Main plugin;

    public CommandForwarder(Logger logger, Main plugin) {
        this.logger = logger;
        this.plugin = plugin;
    }

    public int executeScriptCommands(CommandSender sender, ScriptManager.ScriptConfig script, String[] args) {
        if (isPermissionDenied(sender, script)) {
            return 0;
        }

        for (ScriptManager.Command cmd : script.getCommands()) {
            logger.debug("Processing command: {}", cmd.getCommand());

            switch (cmd.getTargetExecutor().toLowerCase()) {
                case "player" -> handlePlayerExecutor(cmd, sender, args);
                case "console" -> handleConsoleExecutor(cmd, args);
                default -> logger.warn("Unknown target executor for command: {}", cmd.getCommand());
            }
        }

        return 1;
    }

    private boolean isPermissionDenied(CommandSender sender, ScriptManager.ScriptConfig script) {
        if (!script.shouldIgnorePermissionCheck()
                && !sender.hasPermission("commandbridge.command." + script.getName())) {
            logger.warn("Sender '{}' has no permission to use this command", sender);
            if (!script.shouldHidePermissionWarning()) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            }
            return true;
        }
        return false;
    }

    private void handlePlayerExecutor(ScriptManager.Command cmd, CommandSender sender, String[] args) {
        if (cmd.isCheckIfExecutorIsPlayer() && !(sender instanceof Player)) {
            logger.warn("This command requires a player as executor, but sender is not a player.");
            sender.sendMessage(
                    ChatColor.RED + "This command requires a player as executor, but source is not a player object");
            return;
        }

        Player player = (Player) sender;
        String parsedCommand = parseCommand(cmd, args, player);

        if (parsedCommand == null)
            return;

        if (cmd.getDelay() > 0) {
            scheduleCommand(cmd, parsedCommand, player);
        } else {
            sendCommand(cmd, parsedCommand, player);
        }
    }

    private void handleConsoleExecutor(ScriptManager.Command cmd, String[] args) {
        String parsedCommand = parseCommand(cmd, args, null);

        if (parsedCommand == null)
            return;

        if (cmd.getDelay() > 0) {
            scheduleCommand(cmd, parsedCommand, null);
        } else {
            sendCommand(cmd, parsedCommand, null);
        }
    }

    private String parseCommand(ScriptManager.Command cmd, String[] args, Player player) {
        StringParser parser = StringParser.create();

        if (player != null && cmd.getTargetExecutor().equals("player")) {
            addPlayerPlaceholders(parser, player);
        }

        try {
            StringParser.Result result = parser.validate(cmd.getCommand(), args);

            if (!result.isValid()) {
                Set<String> unresolved = result.getUnresolved();

                if (player == null && cmd.getTargetExecutor().equals("console")) {
                    Set<String> playerPlaceholders = new HashSet<>();
                    for (String placeholder : unresolved) {
                        if (isPlayerPlaceholder(placeholder)) {
                            playerPlaceholders.add(placeholder);
                        }
                    }

                    if (!playerPlaceholders.isEmpty()) {
                        logger.error("Console command '{}' contains player placeholders: {}",
                                cmd.getCommand(), playerPlaceholders);

                        Runtime.getInstance().getClient().sendError(
                                "Console command contains unresolvable player placeholders: " + playerPlaceholders);
                        return null;
                    }
                }

                if (!unresolved.isEmpty()) {
                    logger.warn("Command '{}' contains unresolved placeholders: {}", cmd.getCommand(), unresolved);
                }
            }

            String parsedCommand = result.getParsed();

            if (Runtime.getInstance().getStartup().isPlaceholderAPI() && player != null) {
                parsedCommand = PlaceholderAPI.setPlaceholders(player, parsedCommand);
            }

            return parsedCommand;

        } catch (Exception e) {
            logger.error("Error occurred while parsing command: {}", logger.getDebug() ? e : e.getMessage());
            if (player != null) {
                player.sendMessage(ChatColor.RED + "Error occurred while parsing command");
            }
            Runtime.getInstance().getClient().sendError("Error occurred while parsing commands");
            return null;
        }
    }

    private boolean isPlayerPlaceholder(String placeholder) {
        return placeholder.equals("%cb_player%") ||
                placeholder.equals("%cb_uuid%") ||
                placeholder.equals("%cb_world%");
    }

    private void addPlayerPlaceholders(StringParser parser, Player player) {
        logger.debug("Adding placeholders for player: {}", player.getName());
        parser.add("%cb_player%", player.getName());
        parser.add("%cb_uuid%", player.getUniqueId().toString());
        parser.add("%cb_world%", player.getWorld().getName());
    }

    private void scheduleCommand(ScriptManager.Command cmd, String command, Player player) {
        logger.debug("Scheduling command '{}' with delay: {} seconds", cmd.getCommand(), cmd.getDelay());
        if (player != null) {
            player.sendMessage("Scheduling command with '" + cmd.getDelay() + "' seconds");
        }
        new SchedulerAdapter(plugin).runLater(() -> sendCommand(cmd, command, player), cmd.getDelay() * 20L);
    }

    private void sendCommand(ScriptManager.Command cmd, String command, Player player) {
        logger.info("Sending command to server as {}", player == null ? "console" : "player");
        Runtime.getInstance().getClient().sendCommand(command, "", cmd.getTargetExecutor(), player);
    }
}
