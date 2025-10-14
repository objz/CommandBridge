package dev.objz.commandbridge.backends.platform.cmd;

import dev.objz.commandbridge.proto.cmd.CommandStub;

public interface CommandRegistry {
	/**
	 * Register a command from a stub
	 */
	void register(CommandStub stub) throws Exception;

	/**
	 * Unregister all previously registered commands
	 */
	void unregisterAll() throws Exception;
}
