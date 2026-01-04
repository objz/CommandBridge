package dev.objz.commandbridge.backends.platform;

import dev.objz.commandbridge.backends.platform.cmd.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public interface PlatformAdapter {
	record PlatformEnv(Path dataDir) {
	}

	default void load(PlatformEnv env, JavaPlugin plugin) throws Exception {
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
