package dev.objz.commandbridge.backends.platform.cmd;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandExecutor;
import dev.objz.commandbridge.cmd.ArgumentMapper;
import dev.objz.commandbridge.cmd.CommandAPIRegistry;
import dev.objz.commandbridge.proto.cmd.CommandStub;
import dev.objz.commandbridge.scripting.model.enums.Location;

import java.util.function.BiConsumer;

public final class BackendCommandAPIRegistry extends CommandAPIRegistry {

	public BackendCommandAPIRegistry(ArgumentMapper argumentMapper, BiConsumer<String, Object> executionLogger) {
		super(argumentMapper, Location.BACKEND, executionLogger);
	}

	@Override
	protected void registerCommand(CommandAPICommand cmd, String cmdName, CommandStub stub) {
		cmd.executes((CommandExecutor) (sender, args) -> {
			executionLogger.accept(cmdName, sender);

			if (stub.args() != null && !stub.args().isEmpty()) {
				logArguments(stub.args(), args);
			}
		});
	}
}
