package dev.objz.commandbridge.backends.platform;

import dev.objz.commandbridge.backends.platform.cmd.CommandExecutor;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

public interface PlatformAdapter<P> {
    record PlatformEnv(Path dataDir, String configName) {
        public PlatformEnv(Path dataDir) {
            this(dataDir, "config.yml");
        }
    }

    default void load(PlatformEnv env, P plugin) throws Exception {
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
     * Get the UUIDs of all online players on this server/proxy.
     *
     * @return Set of online player UUIDs
     */
    Set<UUID> getOnlinePlayerIds();

    /**
     * Schedules a repeating task that cancels itself after the total timeout duration is reached.
     *
     * @param task      The task to run.
     * @param timeout   The total duration after which the task should stop running.
     * @param interval  The delay between executions (frequency).
     * @return A handle for early cancellation.
     */
    ScheduleHandle runSchedule(Runnable task, Duration timeout, Duration interval);
}
