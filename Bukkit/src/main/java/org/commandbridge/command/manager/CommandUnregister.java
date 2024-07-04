package org.commandbridge.command.manager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.commandbridge.CommandBridge;
import org.commandbridge.utilities.VerboseLogger;

import java.lang.reflect.Field;
import java.util.Map;

public class CommandUnregister {
    private final CommandBridge plugin;
    private final VerboseLogger verboseLogger;


    public CommandUnregister(CommandBridge plugin) {
        this.plugin = plugin;
        this.verboseLogger = plugin.getVerboseLogger();
    }

    public void unregisterCommand(String command) {
        try {
            Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(plugin.getServer());

            if (commandMap instanceof SimpleCommandMap) {
                SimpleCommandMap simpleCommandMap = (SimpleCommandMap) commandMap;

                Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
                knownCommandsField.setAccessible(true);
                Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(simpleCommandMap);

                if (knownCommands.containsKey(command)) {
                    knownCommands.remove(command);

                    knownCommands.values().removeIf(cmd -> cmd instanceof PluginCommand && ((PluginCommand) cmd).getPlugin() == plugin);

                    verboseLogger.info("Successfully unregistered command: " + command);
                } else {
                    verboseLogger.warn("Command not found: " + command);
                }
            }

        } catch (NoSuchFieldException | IllegalAccessException e) {
            verboseLogger.error("Failed to unregister command: " + command, e);
        }
    }
}
