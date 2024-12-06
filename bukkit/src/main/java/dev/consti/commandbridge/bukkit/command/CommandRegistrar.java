package dev.consti.commandbridge.bukkit.command;

import java.util.ArrayList;
import java.util.List;

import dev.consti.commandbridge.bukkit.core.Runtime;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.foundationlib.utils.ScriptManager;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;

public class CommandRegistrar {
    private final Logger logger;
    private final CommandHelper helper;
    private final List<String> registeredCommands = new ArrayList<>();

    public CommandRegistrar(Logger logger) {
        this.logger = logger;
        this.helper = Runtime.getInstance().getHelper();
    }

    

public void unregisterAllCommands() {
        logger.debug("Starting the process of unregistering all commands...");
        for (String command : registeredCommands) {
            try {
                CommandAPI.unregister(command);
                logger.debug("Successfully unregistered command: {}", command);
            } catch (Exception e) {
                logger.error("Failed to unregister command: {}. Error: {}", command, e);
            }
        }
        registeredCommands.clear();
        logger.info("All registered commands have been unregistered.");
    
}

public void registerCommand(ScriptManager.ScriptConfig script) {
        String commandName = script.getName();
        logger.debug("Starting registration for command: {}", commandName);

        try {
            CommandAPICommand command = new CommandAPICommand(commandName)
                    .executes((sender, args) -> {
                        logger.debug("Executing base command: {}", commandName);
                        return helper.executeScriptCommands(sender, script, new String[0]);
                    })
                    .withOptionalArguments(new GreedyStringArgument("args"))
                    .executes((sender, args) -> {
                        String argsString = (String) args.get("args");
                        logger.debug("Command {} called with arguments: {}", commandName, argsString);
                        String[] splitArgs = argsString != null ? argsString.split(" ") : new String[0];
                        return helper.executeScriptCommands(sender, script, splitArgs);
                    });

            command.register();
            registeredCommands.add(commandName);
            logger.info("Command {} registered successfully.", commandName);
        } catch (Exception e) {
            logger.error("Failed to register command {}. Error: {}", commandName, e.getMessage(), e);
        }
}

}
