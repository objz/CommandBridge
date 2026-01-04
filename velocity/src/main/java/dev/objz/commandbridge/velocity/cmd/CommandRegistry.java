package dev.objz.commandbridge.velocity.cmd;

import com.velocitypowered.api.command.CommandSource;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.objz.commandbridge.cmd.CommandRegistryInterface;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.payloads.cmd.CommandStub;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CommandRegistry implements CommandRegistryInterface {

	@FunctionalInterface
	public interface CommandExecutionHandler {
		void execute(String commandName, CommandSource source, CommandArguments args, CommandStub stub);
	}

	private final Set<String> registeredCommands = ConcurrentHashMap.newKeySet();
	private final Set<String> registeredAliases = ConcurrentHashMap.newKeySet();
	private final ArgumentMapper argumentMapper;
	private final Object registrationLock = new Object();
	private final CommandExecutionHandler executionHandler;

	public CommandRegistry(ArgumentMapper mapper) {
		this(mapper, null);
	}

	public CommandRegistry(ArgumentMapper mapper, CommandExecutionHandler executionHandler) {
		this.argumentMapper = mapper;
		this.executionHandler = executionHandler;
	}

	@Override
	public void register(CommandStub stub) throws Exception {
		String cmdName = stub.name();

		synchronized (registrationLock) {
			if (registeredCommands.contains(cmdName)) {
				Log.debug("Command '{}' is already registered, it will be replaced", cmdName);
			}

			CommandAPICommand cmd = new CommandAPICommand(cmdName);

			if (stub.description() != null && !stub.description().isBlank()) {
				cmd.withShortDescription(stub.description());
				cmd.withFullDescription(stub.description());
			}

			if (stub.aliases() != null && !stub.aliases().isEmpty()) {
				cmd.withAliases(stub.aliases().toArray(new String[0]));
				registeredAliases.addAll(stub.aliases());
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
				CommandSource source = (CommandSource) sender;

				if (executionHandler != null) {
					executionHandler.execute(cmdName, source, args, stub);
				} else {
					Log.info("Command '{}' executed by {} (no handler configured)", cmdName,
							sender.toString());

					if (stub.args() != null && !stub.args().isEmpty()) {
						StringBuilder argLog = new StringBuilder("Arguments: ");
						boolean first = true;
						for (ArgMapping argMapping : stub.args()) {
							if (!first)
								argLog.append(", ");
							first = false;
							Object value = args.getOptional(argMapping.name()).orElse(null);
							argLog.append(argMapping.name()).append("=").append(
									value != null ? value.toString()
											: "<not provided>");
						}
						Log.info(argLog.toString());
					}
				}
			});

			cmd.register();
			registeredCommands.add(cmdName);

			int argCount = stub.args() != null ? stub.args().size() : 0;
			int aliasCount = stub.aliases() != null ? stub.aliases().size() : 0;
			Log.debug("Registered command '{}' with {} arg(s) and {} alias(es)", cmdName,
					argCount, aliasCount);
		}
	}

	@Override
	public void unregisterAll() throws Exception {
		synchronized (registrationLock) {
			if (registeredCommands.isEmpty()) {
				return;
			}
			Set<String> toUnregister = new HashSet<>(registeredCommands);
			Log.info("Unregistering {} command(s)...", toUnregister.size());
			toUnregister.addAll(registeredAliases);
			for (String cmdName : toUnregister) {
				try {
					CommandAPI.unregister(cmdName, true);
					Log.debug("Unregistered command '{}'", cmdName);
				} catch (Exception e) {
					Log.warn("Failed to unregister command '{}': {}", cmdName, e.getMessage());
				}
			}
			registeredCommands.clear();
		}
	}
}
