package dev.objz.commandbridge.backends.platform.cmd;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.objz.commandbridge.cmd.ArgumentMapperInterface; // <- fixed import
import dev.objz.commandbridge.cmd.CommandRegistryInterface;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.proto.cmd.CommandStub;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CommandRegistry implements CommandRegistryInterface {

	private final List<String> registeredCommands = new CopyOnWriteArrayList<>();
	private final ArgumentMapperInterface<Argument<?>> argumentMapper; // <- typed

	public CommandRegistry(ArgumentMapperInterface<Argument<?>> mapper) { // <- typed
		this.argumentMapper = mapper;
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
			cmd.withAliases(stub.aliases().toArray(String[]::new));
		}

		if (stub.args() != null && !stub.args().isEmpty()) {
			List<Argument<?>> arguments = new ArrayList<>(stub.args().size());

			for (ArgMapping argMapping : stub.args()) {
				Argument<?> argument = argumentMapper.map(argMapping);

				if (!argMapping.required()) {
					argument.setOptional(true);
				}

				arguments.add(argument);
			}

			cmd.withArguments(arguments);
		}

		cmd.executes((sender, args) -> {
			Log.info("Command '{}' executed by {}", cmdName, sender.toString());

			if (stub.args() != null && !stub.args().isEmpty()) {
				StringBuilder argLog = new StringBuilder("Arguments: ");
				boolean first = true;

				for (ArgMapping argMapping : stub.args()) {
					if (!first)
						argLog.append(", ");
					first = false;
					Object value = args.getOptional(argMapping.name()).orElse(null);
					argLog.append(argMapping.name()).append("=")
							.append(value != null ? value.toString() : "<not provided>");
				}

				Log.info(argLog.toString());
			}
		});

		cmd.register();
		registeredCommands.add(cmdName);

		int argCount = stub.args() != null ? stub.args().size() : 0;
		int aliasCount = stub.aliases() != null ? stub.aliases().size() : 0;
		Log.debug("Registered command '{}' with {} arg(s) and {} alias(es)", cmdName, argCount, aliasCount);
	}

	@Override
	public void unregisterAll() throws Exception {
		for (String cmdName : registeredCommands) {
			try {
				CommandAPI.unregister(cmdName);
			} catch (Exception e) {
				Log.warn("Failed to unregister command '{}': {}", cmdName, e.getMessage());
			}
		}
		registeredCommands.clear();
	}
}
