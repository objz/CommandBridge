package org.commandbridge.command.manager;

import org.bukkit.command.*;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.commandbridge.CommandBridge;
import org.commandbridge.utilities.VerboseLogger;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.commandbridge.utilities.StringParser.parsePlaceholders;
import static org.commandbridge.utilities.StringParser.parseConsoleCommands;

public class CommandRegister {
    private final CommandBridge plugin;
    private final VerboseLogger verboseLogger;

    public CommandRegister(CommandBridge plugin) {
        this.plugin = plugin;
        this.verboseLogger = plugin.getVerboseLogger();
    }

    public void registerCommands(Map<String, Object> data) {
        String commandName = (String) data.get("name");

        List<Map<String, Object>> commandList = safeCastToListOfMaps(data.get("commands"));

        if (commandName == null || commandList == null || commandList.isEmpty()) {
            verboseLogger.error("Command name or command list is missing or empty in config.", new IllegalArgumentException());
            return;
        }

        try {
            Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(plugin.getServer());

            Command newCommand = new BukkitCommand(commandName) {
                @Override
                public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, String[] args) {
                    verboseLogger.info("Executing command: " + commandLabel + " with arguments: " + String.join(" ", args));

                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        for (Map<String, Object> command : commandList) {
                            String commandString = parsePlaceholders((String) command.get("command"), player);
                            String targetExecutor = (String) command.get("target-executor");
                            String permission = "commandbridge.command." + commandString;

                            if (!sender.hasPermission(permission)) {
                                verboseLogger.warn("Player " + player.getName() + " does not have permission to execute command: " + commandString);
                                return false;
                            }
                            verboseLogger.info("Sending plugin message for command as Player: " + commandString);
                            plugin.getMessageSender().sendPluginMessage(player.getUniqueId().toString(), targetExecutor, commandString);
                        }
                        return true;
                    } else if (sender instanceof ConsoleCommandSender) {
                        for (Map<String, Object> command : commandList) {
                            String commandString = parseConsoleCommands((String) command.get("command"), (ConsoleCommandSender) sender);
                            String targetExecutor = (String) command.get("target-executor");
                            verboseLogger.info("Sending plugin message for command as Console: " + commandString);
                            plugin.getMessageSender().sendPluginMessage("", targetExecutor, commandString); // Assuming null player for console
                        }
                        return true;
                    } else {
                        verboseLogger.warn("This command can only be used by a player or console.");
                    }
                    return false;
                }
            };

            commandMap.register(plugin.getName(), newCommand);
            verboseLogger.forceInfo("Command registered successfully: " + commandName);
        } catch (Exception e) {
            verboseLogger.error("Failed to register command: " + commandName, e);
        }

        plugin.addRegisteredCommand(commandName);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> safeCastToListOfMaps(Object obj) {
        if (obj instanceof List<?>) {
            List<?> list = (List<?>) obj;
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                try {
                    return (List<Map<String, Object>>) obj;
                } catch (ClassCastException e) {
                    verboseLogger.error("Failed to cast to List<Map<String, Object>>", e);
                }
            }
        }
        return null;
    }
}
