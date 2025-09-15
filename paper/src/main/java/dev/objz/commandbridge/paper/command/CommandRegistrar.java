package dev.objz.commandbridge.paper.command;

import java.util.ArrayList;
import java.util.List;

import dev.objz.commandbridge.core.Logger;
import dev.objz.commandbridge.core.utils.ScriptManager;
import dev.objz.commandbridge.paper.command.outbound.CommandSender;
import dev.objz.commandbridge.paper.core.Runtime;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;

public class CommandRegistrar {
	private final Logger logger;
	private final List<String> registeredCommands = new ArrayList<>();
	private final CommandSender bridgeSender;

	public CommandRegistrar(Logger logger) {
		this.logger = logger;
		this.bridgeSender = Runtime.getInstance().getSender();
	}

	public void unregisterAllCommands() {
		for (String command : registeredCommands) {
			try {
				CommandAPI.unregister(command);
				logger.debug("Unregistered command: {}", command);
			} catch (Exception e) {
				logger.error("Failed to unregister command '{}' : {}", command,
						logger.getDebug() ? e : e.getMessage());
			}
		}
		registeredCommands.clear();
		logger.info("All registered commands have been unregistered.");
	}

	public void registerCommand(ScriptManager.ScriptConfig script) {
		final String commandName = script.getName();

		try {
			CommandAPICommand command = new CommandAPICommand(commandName)
					.withOptionalArguments(new GreedyStringArgument("args"))
					.withAliases(script.getAliases().toArray(new String[0]))
					.executes((sender, args) -> {
						String argsString = (String) args.get("args");
						logger.debug("Command '{}' called with arguments: {}", commandName,
								argsString);

						String[] splitArgs = (argsString == null || argsString.isBlank())
								? new String[0]
								: argsString.split(" ");

						return bridgeSender.executeScriptCommands(sender, script, splitArgs);
					});

			command.register();
			registeredCommands.add(commandName);
			logger.debug("Registered command: {}", commandName);
		} catch (Exception e) {
			logger.error("Failed to register command '{}' : {}", commandName,
					logger.getDebug() ? e : e.getMessage());
		}
	}
}
