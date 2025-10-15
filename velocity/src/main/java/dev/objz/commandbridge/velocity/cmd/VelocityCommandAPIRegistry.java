package dev.objz.commandbridge.velocity.cmd;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.objz.commandbridge.cmd.ArgumentMapper;
import dev.objz.commandbridge.cmd.CommandRegistry;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.proto.cmd.CommandStub;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class VelocityCommandAPIRegistry implements CommandRegistry {

	private final List<String> registeredCommands = new CopyOnWriteArrayList<>();
	private final ArgumentMapper argumentMapper;

	public VelocityCommandAPIRegistry(ArgumentMapper mapper) {
		this.argumentMapper = mapper;
	}

	@Override
	public void register(CommandStub stub) throws Exception {
		String cmdName = stub.name();

		//TODO
		//

		new CommandAPICommand(cmdName)
			.withArguments(new IntegerArgument("max"))
			.withArguments(new IntegerArgument("min"))
			.register();


		}

	@Override
	public void unregisterAll() throws Exception {
		//TODO
	}
}
