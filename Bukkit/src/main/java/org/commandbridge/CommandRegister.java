package org.commandbridge;



import org.bukkit.command.*;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

public class CommandRegister {
    private final CommandBridge plugin;
    private final VerboseLogger verboseLogger;


    public CommandRegister(CommandBridge plugin) {
        this.plugin = plugin;
        this.verboseLogger = plugin.getVerboseLogger();
    }

    public void registerCommand(String command) {
        try {
            Field comandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            comandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) comandMapField.get(plugin.getServer());

            Command newCommand = new BukkitCommand(command) {
                @Override
                public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                    if (sender instanceof Player) {
                        plugin.getMessageSender().sendPluginMessage(command);
                        return true;
                    } else {
                        verboseLogger.warn("This command can only be used by a player.");
                    }
                    return false;
                }
            };
            commandMap.register(plugin.getName(), newCommand);
        } catch (Exception e) {
            verboseLogger.error("Failed to register command: " + command, e);
        }
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