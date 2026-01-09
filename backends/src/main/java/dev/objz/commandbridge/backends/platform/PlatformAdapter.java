package dev.objz.commandbridge.backends.platform;

import dev.objz.commandbridge.backends.platform.cmd.CommandExecutor;

import java.nio.file.Path;

public interface PlatformAdapter {
	record PlatformEnv(Path dataDir) {
	}

	default void load(PlatformEnv env, Object plugin) throws Exception {
	}

	void start(PlatformEnv env) throws Exception;

	void stop() throws Exception;

	/**
	 * Get the platform-specific command executor.
	 * 
	 * @return CommandExecutor for this platform
	 */
	CommandExecutor getCommandExecutor();
}
