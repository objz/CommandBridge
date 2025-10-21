package dev.objz.commandbridge.cmd;

import dev.objz.commandbridge.proto.cmd.CommandStub;

public interface CommandRegistryInterface {
	void register(CommandStub stub) throws Exception;

	void unregisterAll() throws Exception;
}
