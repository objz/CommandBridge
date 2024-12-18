package dev.consti.commandbridge.bukkit.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.consti.commandbridge.bukkit.Main;
import dev.consti.commandbridge.bukkit.core.Runtime;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.foundationlib.utils.ScriptManager;
import dev.consti.foundationlib.utils.StringParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;


public class CommandHelper {
    private final Logger logger;
    private final Main plugin;

    public CommandHelper(Logger logger, Main plugin) {
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
            logger.warn("Permission check failed for sender: {}", sender.getName());
            if (!script.shouldHidePermissionWarning()) {
                sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            }
            return true;
        }
        return false;
    }

    private void handlePlayerExecutor(ScriptManager.Command cmd, CommandSender sender, String[] args) {
        if (cmd.isCheckIfExecutorIsPlayer() && !(sender instanceof Player)) {
            logger.warn("This command requires a player as executor, but sender is not a player.");
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
            String parsedCommand = parser.parsePlaceholders(cmd.getCommand(), args);
            return parsedCommand;

        } catch (Exception e) {
            logger.error("Error occurred while parsing command: {}", logger.getDebug() ? e : e.getMessage());
            if (player != null) {
                player.sendMessage(Component.text("Error occurred while parsing command").color(NamedTextColor.RED));
            }
            Runtime.getInstance().getClient().sendError("Error occurred while parsing commands");
        }

        return null;
    }

    private void addPlayerPlaceholders(StringParser parser, Player player) {
        logger.debug("Adding placeholders for player: {}", player.getName());
        parser.addPlaceholder("%player%", player.getName());
        parser.addPlaceholder("%uuid%", player.getUniqueId().toString());
        parser.addPlaceholder("%world%", player.getWorld().getName());
    }

    private void scheduleCommand(ScriptManager.Command cmd, String command, Player player) {
        logger.debug("Scheduling command '{}' with delay: {} seconds", cmd.getCommand(), cmd.getDelay());
        Bukkit.getScheduler().runTaskLater(plugin, () -> sendCommand(cmd, command, player), cmd.getDelay() * 20L);
    }

    private void sendCommand(ScriptManager.Command cmd, String command, Player player) {
        // List<String> targetServers = cmd.getTargetServerIds();
        //
        // if (targetServers.isEmpty()) {
        // logger.warn("No target servers defined for command: {}", cmd.getCommand());
        // return;
        // }
        //
        // for (String serverId : targetServers) {
        // try {
        // logger.info("Sending command to server '{}' as {}", serverId, player == null
        // ? "console" : "player");
        // Runtime.getInstance().getClient().sendCommand(command, serverId,
        // cmd.getTargetExecutor(), player);
        // } catch (Exception e) {
        // logger.error("Failed to send command to server '{}': {}", serverId,
        // e.getMessage());
        // }
        // }

        logger.info("Sending command to server as {}", player == null ? "console" : "player");
        Runtime.getInstance().getClient().sendCommand(command, "", cmd.getTargetExecutor(), player);
    }
}
