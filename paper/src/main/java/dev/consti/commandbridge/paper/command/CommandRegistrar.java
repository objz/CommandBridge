package dev.consti.commandbridge.paper.command;

import java.util.ArrayList;
import java.util.List;

import dev.consti.commandbridge.paper.core.Runtime;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.foundationlib.utils.ScriptManager;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;

public class CommandRegistrar {
    private final Logger logger;
    private final CommandForwarder forwarder;
    private final List<String> registeredCommands = new ArrayList<>();

    public CommandRegistrar(Logger logger) {
        this.logger = logger;
        this.forwarder = Runtime.getInstance().getForwarder();
    }

    

public void unregisterAllCommands() {
        for (String command : registeredCommands) {
            try {
                CommandAPI.unregister(command);
                logger.debug("Unregistered command: {}", command);
            } catch (Exception e) {
                logger.error("Failed to unregister command '{}' : {}",
                        command,
                        logger.getDebug() ? e : e.getMessage()
                );
            }
        }
        registeredCommands.clear();
        logger.info("All registered commands have been unregistered.");
    
}

public void registerCommand(ScriptManager.ScriptConfig script) {
        String commandName = script.getName();
        try {
            CommandAPICommand command = new CommandAPICommand(commandName)
                    .withOptionalArguments(new GreedyStringArgument("args"))
                            .executes((sender, args) -> {
                                String argsString = (String) args.get("args");
                                logger.debug("Command '{}' called with arguments: {}", commandName, argsString);
                                String[] splitArgs = argsString != null ? argsString.split(" ") : new String[0];
                                return forwarder.executeScriptCommands(sender, script, splitArgs);
                            });
            command.register();
            registeredCommands.add(commandName);
        } catch (Exception e) {
            logger.error(
                    "Failed to register command '{}' : {}",
                    commandName,
                    logger.getDebug() ? e : e.getMessage()
            );
        }
}

}
