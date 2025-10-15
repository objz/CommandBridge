package dev.objz.commandbridge.cmd;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.proto.cmd.CommandStub;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public class CommandAPIRegistry implements CommandRegistry {
	private final List<String> registeredCommands = new CopyOnWriteArrayList<>();
	protected final ArgumentMapper argumentMapper;
	protected final BiConsumer<String, Object> executionLogger;
	protected final Location location;

	public CommandAPIRegistry(ArgumentMapper argumentMapper, Location location) {
		this(argumentMapper, location, null);
	}

	public CommandAPIRegistry(ArgumentMapper argumentMapper, Location location, BiConsumer<String, Object> executionLogger) {
		this.argumentMapper = argumentMapper;
		this.location = location;
		this.executionLogger = executionLogger != null ? executionLogger
				: (cmd, sender) -> Log.info("Command '{}' executed by {}", cmd, sender);
	}

	@Override
	public void register(CommandStub stub) throws Exception {
		String cmdName = stub.name();

		CommandAPICommand cmd = new CommandAPICommand(cmdName);

		if (stub.description() != null && !stub.description().isBlank()) {
			cmd.withShortDescription(stub.description());
			cmd.withFullDescription(stub.description());
		}

		if (stub.aliases() != null && !stub.aliases().isEmpty()) {
			cmd.withAliases(stub.aliases().toArray(new String[0]));
		}

		if (stub.args() != null && !stub.args().isEmpty()) {
			List<Argument<?>> arguments = new ArrayList<>(stub.args().size());

			for (ArgMapping argMapping : stub.args()) {
				try {
					Argument<?> argument = argumentMapper.map(argMapping);

					if (!argMapping.required()) {
						argument.setOptional(true);
					}

					arguments.add(argument);
				} catch (UnsupportedOperationException e) {
					Log.warn("Skipping unsupported argument '{}' of type {} for command '{}'",
							argMapping.name(), argMapping.type(), cmdName);
					throw new Exception("Unsupported argument type: " + argMapping.type()
							+ " for argument: " + argMapping.name());
				}
			}

			cmd.withArguments(arguments);
		}

		// Platform-specific registration
		registerCommand(cmd, cmdName, stub);

		cmd.register();
		registeredCommands.add(cmdName);

		int argCount = stub.args() != null ? stub.args().size() : 0;
		int aliasCount = stub.aliases() != null ? stub.aliases().size() : 0;

		Log.debug("Registered command '{}' with {} argument(s) and {} alias(es)",
				cmdName, argCount, aliasCount);
	}

	protected void registerCommand(CommandAPICommand cmd, String cmdName, CommandStub stub) {
		// Default: Velocity implementation using executesNative
		cmd.executesNative((sender, args) -> {
			executionLogger.accept(cmdName, sender);

			if (stub.args() != null && !stub.args().isEmpty()) {
				logArguments(stub.args(), args);
			}

			return 1; // Success
		});
	}

	protected void logArguments(List<ArgMapping> argMappings, dev.jorel.commandapi.executors.CommandArguments args) {
		StringBuilder argLog = new StringBuilder("Arguments: ");
		boolean first = true;

		for (ArgMapping argMapping : argMappings) {
			if (!first) {
				argLog.append(", ");
			}
			first = false;

			Object value = args.getOptional(argMapping.name()).orElse(null);

			argLog.append(argMapping.name())
					.append("=")
					.append(value != null ? value.toString() : "<not provided>");
		}

		Log.info(argLog.toString());
	}

	@Override
	public void unregisterAll() throws Exception {
		int count = 0;
		for (String cmdName : registeredCommands) {
			try {
				dev.jorel.commandapi.CommandAPI.unregister(cmdName);
				count++;
			} catch (Exception e) {
				Log.warn("Failed to unregister command '{}': {}", cmdName, e.getMessage());
			}
		}
		registeredCommands.clear();

		if (count > 0) {
			Log.info("Unregistered {} command(s)", count);
		}
	}
}
