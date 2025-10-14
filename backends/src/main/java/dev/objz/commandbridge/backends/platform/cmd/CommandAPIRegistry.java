package dev.objz.commandbridge.backends.platform.cmd;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.proto.cmd.CommandStub;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CommandAPIRegistry implements CommandRegistry {

	private final List<String> registeredCommands = new CopyOnWriteArrayList<>();
	private final ArgumentMapper argumentMapper = new ArgumentMapper();

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
				Argument<?> argument = argumentMapper.map(argMapping.name(), argMapping.type());

				if (!argMapping.required()) {
					argument.setOptional(true);
				}

				if (argMapping.suggestions() != null && !argMapping.suggestions().isEmpty()) {
					argument.includeSuggestions(ArgumentSuggestions.strings(
							argMapping.suggestions().toArray(String[]::new)));
				}

				arguments.add(argument);
			}

			cmd.withArguments(arguments);
		}

		cmd.executes((sender, args) -> {
			Log.info("Command '{}' executed by {}", cmdName, sender.getName());

			if (stub.args() != null && !stub.args().isEmpty()) {
				StringBuilder argLog = new StringBuilder("Arguments: ");
				boolean first = true;

				for (ArgMapping argMapping : stub.args()) {
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
		});

		cmd.register();
		registeredCommands.add(cmdName);

		int argCount = stub.args() != null ? stub.args().size() : 0;
		int aliasCount = stub.aliases() != null ? stub.aliases().size() : 0;

		Log.debug("Registered command '{}' with {} argument(s) and {} alias(es)",
				cmdName, argCount, aliasCount);
	}

	@Override
	public void unregisterAll() throws Exception {
		int count = 0;
		for (String cmdName : registeredCommands) {
			try {
				CommandAPI.unregister(cmdName);
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
