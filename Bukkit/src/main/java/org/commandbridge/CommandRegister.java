package org.commandbridge;



import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

public class CommandRegister {
    private final CommandBridge plugin;


    public CommandRegister(CommandBridge plugin) {
        this.plugin = plugin;
    }

    public void registerCommand(String command) {
        try {
            Field comandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            comandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) comandMapField.get(plugin.getServer());

            Command newCommand = new BukkitCommand(command) {
                @Override
                public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                    sender.sendMessage("Command executed");
                    return false;
                }
            };
            commandMap.register(plugin.getName(), newCommand);
        } catch (Exception e) {
            plugin.getVerboseLogger().error("Failed to register command: " + command, e);
        }
    }

    public void unregisterCommand(String commandName) {
        VerboseLogger logger = plugin.getVerboseLogger();
        logger.info("Unregistering command: " + commandName);
        try {
            Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(plugin.getServer());
            logger.info("CommandMap: " + commandMap);

            // Get the knownCommands map from the CommandMap
            Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
            logger.info("knownCommands: " + knownCommands);
            // Remove the command from the knownCommands map
            if (knownCommands.remove(commandName) != null) {
                logger.info("Successfully unregistered command: " + commandName);
            } else {
                logger.warn("Command not found: " + commandName);
            }
        } catch (Exception e) {
            logger.error("Failed to unregister command: " + commandName, e);
        }

        }

}