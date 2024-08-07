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
import java.util.Objects;

import static org.commandbridge.utilities.StringParser.*;

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
                        verboseLogger.info("Command sender is a player: " + player.getName());
                        handlePlayerCommand(player, commandList, args);
                        return true;
                    } else if (sender instanceof BlockCommandSender) {
                        verboseLogger.info("Command sender is a command block.");
                        handleCommandBlockCommand((BlockCommandSender) sender, commandList, args);
                        return true;
                    } else if (sender instanceof ConsoleCommandSender) {
                        verboseLogger.info("Command sender is the console.");
                        handleConsoleCommand((ConsoleCommandSender) sender, commandList, args);
                        return true;
                    } else {
                        verboseLogger.warn("This command can only be used by a player, console, or command block.");
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

    private void handlePlayerCommand(Player player, List<Map<String, Object>> commandList, String[] args) {
        verboseLogger.info("Player command sender: " + player.getName());
        for (Map<String, Object> command : commandList) {
            String commandString = parsePlaceholders((String) command.get("command"), player, args);
            String targetExecutor = (String) command.get("target-executor");
            List<String> targetServerIds = safeCastToListOfStrings(command.get("target-server-ids"));
            String permission = "commandbridge.command." + commandString;

            if (targetServerIds == null) {
                verboseLogger.warn("Target server IDs are not specified or invalid for command: " + commandString);
                continue;
            }

            if (!player.hasPermission(permission)) {
                verboseLogger.warn("Player " + player.getName() + " does not have permission to execute command: " + commandString);
                return;
            }
            verboseLogger.info("Sending plugin message for command as Player: " + commandString);
            for (String targetServerId : targetServerIds) {
                plugin.getMessageSender().sendPluginMessage(player.getUniqueId().toString(), targetExecutor, commandString, targetServerId);
            }
        }
    }

    private void handleConsoleCommand(ConsoleCommandSender sender, List<Map<String, Object>> commandList, String[] args) {
        verboseLogger.info("Console command sender: " + sender.getName());
        for (Map<String, Object> command : commandList) {
            String commandString = parseConsoleCommands((String) command.get("command"), sender, args);
            String targetExecutor = (String) command.get("target-executor");
            List<String> targetServerIds = safeCastToListOfStrings(command.get("target-server-ids"));

            if (targetServerIds == null) {
                verboseLogger.warn("Target server IDs are not specified or invalid for command: " + commandString);
                continue;
            }

            verboseLogger.info("Sending plugin message for command as Console: " + commandString);
            for (String targetServerId : targetServerIds) {
                plugin.getMessageSender().sendPluginMessage("", targetExecutor, commandString, targetServerId); // Assuming null player for console
            }
        }
    }

    private void handleCommandBlockCommand(BlockCommandSender sender, List<Map<String, Object>> commandList, String[] args) {
        verboseLogger.info("Command block command sender: " + sender.getName());
        for (Map<String, Object> command : commandList) {
            String commandString = parseBlockCommands((String) command.get("command"), sender, args);
            String targetExecutor = (String) command.get("target-executor");
            List<String> targetServerIds = safeCastToListOfStrings(command.get("target-server-ids"));

            if (targetServerIds == null) {
                verboseLogger.warn("Target server IDs are not specified or invalid for command: " + commandString);
                continue;
            }

            verboseLogger.info("Sending plugin message for command as CommandBlock: " + commandString);
            for (String targetServerId : targetServerIds) {
                plugin.getMessageSender().sendPluginMessage((Objects.requireNonNull(sender.getServer().getPlayer(sender.getName())).getUniqueId().toString()), targetExecutor, commandString, targetServerId); // Assuming null player for command block
            }
        }
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

    @SuppressWarnings("unchecked")
    private List<String> safeCastToListOfStrings(Object obj) {
        if (obj instanceof List<?>) {
            List<?> list = (List<?>) obj;
            if (!list.isEmpty() && list.get(0) instanceof String) {
                try {
                    return (List<String>) obj;
                } catch (ClassCastException e) {
                    verboseLogger.error("Failed to cast to List<String>", e);
                }
            }
        }
        return null;
    }
}
