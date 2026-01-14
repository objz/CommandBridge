package dev.objz.commandbridge.backends.platform;

import dev.objz.commandbridge.backends.platform.cmd.CommandExecutor;

import java.nio.file.Path;
import java.time.Duration;

public interface PlatformAdapter {
	record PlatformEnv(Path dataDir, String configName) {
		public PlatformEnv(Path dataDir) {
			this(dataDir, "config.yml");
		}
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

	/**
	 * Schedules a repeating task that cancels itself after the total timeout duration is reached.
	 *
	 * @param task      The task to run.
	 * @param timeout   The total duration after which the task should stop running.
	 * @param interval  The delay between executions (frequency).
	 * @return An object representing the scheduled task (for early cancellation).
	 */
	Object runSchedule(Runnable task, Duration timeout, Duration interval);

	/**
	 * @param task The task object returned by runSchedule.
	 */
	void cancelSchedule(Object task);
}
