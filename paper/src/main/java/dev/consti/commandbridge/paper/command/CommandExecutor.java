package dev.consti.commandbridge.paper.command;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.consti.commandbridge.paper.Main;
import dev.consti.commandbridge.paper.core.Runtime;
import dev.consti.commandbridge.paper.utils.CommandUtils;
import dev.consti.commandbridge.paper.utils.SchedulerAdapter;
import dev.consti.foundationlib.json.MessageParser;
import dev.consti.foundationlib.logging.Logger;

public class CommandExecutor {
    private final Main plugin;
    private final Logger logger;

    public CommandExecutor() {
        this.plugin = Main.getInstance();
        this.logger = Runtime.getInstance().getLogger();
    }

    public void dispatchCommand(String message) {
        MessageParser parser = new MessageParser(message);
        String serverId = Runtime.getInstance().getConfig().getKey("config.yml", "client-id");
        if (!parser.getBodyValueAsString("client").equals(serverId)) {
            logger.debug("Message not intended for this client: {}", serverId);
            return;
        }
        String command = parser.getBodyValueAsString("command");
        String target = parser.getBodyValueAsString("target");
        logger.info("Dispatching command '{}' for executor: {}", command, target);

        switch (target) {
            case "console" -> executeConsoleCommand(command);
            case "player" -> executePlayerCommand(parser, command);
            default -> logger.warn("Invalid target: {}", target);
        }
    }

    private void executeConsoleCommand(String command) {
        logger.debug("Executing command '{}' as console", command);

        if (CommandUtils.isCommandValid(command)) {
            logger.warn("Invalid command: {}", command);
            Runtime.getInstance().getClient().sendError("Invalid command: " + command);
            return;
        }

        CommandSender console = Bukkit.getConsoleSender();
        new SchedulerAdapter(plugin).run(() -> {
            boolean status = Bukkit.dispatchCommand(console, command);
            logResult("console", command, status);
        });
   }

    private void executePlayerCommand(MessageParser parser, String command) {
        logger.debug("Executing command '{}' as player", command);
        String uuidStr = parser.getBodyValueAsString("uuid");
        String name = parser.getBodyValueAsString("name");

        try {
            UUID uuid = UUID.fromString(uuidStr);
            Optional<Player> playerOptional = Optional.ofNullable(Bukkit.getPlayer(uuid));

            playerOptional.ifPresentOrElse(player -> handlePlayerCommand(player, command),
                    () -> logger.warn("Player '{}' not found or offline", name));
        } catch (Exception e) {
            logger.error("Error while processing player: {}",
                    logger.getDebug() ? e : e.getMessage()
                    );
            Runtime.getInstance().getClient().sendError("Error while processing player: " + e.getMessage());
        }
   }

private void handlePlayerCommand(Player player, String command) {
    if (CommandUtils.isCommandValid(command)) {
        logger.warn("Invalid command: {}", command);
        Runtime.getInstance().getClient().sendError("Invalid command: " + command);
        player.sendMessage("§cThe command '" + command + "' is invalid");
        return;
    }

    new SchedulerAdapter(plugin).run(() -> {
      boolean status = Bukkit.dispatchCommand(player, command);
      logResult("player", command, status);
    });
}


// private boolean isCommandValid(String command) {
//     String baseCommand = command.split(" ")[0];
//     PluginCommand pluginCommand = Bukkit.getPluginCommand(baseCommand);
//     if (pluginCommand != null) {
//         return false;
//     }
//     return Bukkit.getServer().getCommandMap().getCommand(baseCommand) == null;
// }
//
private void logResult(String target, String command, boolean status) {
    if (status) {
        logger.info("Successfully executed command '{}' as {}", command, target);
    } else {
        logger.warn("Failed to execute command '{}' as {}", command, target);
        Runtime.getInstance().getClient().sendError("Failed to execute command '" + command + "' as " + target);
    }
}

}

