package dev.objz.commandbridge.backends.platform;

/**
 * A handle to a scheduled repeating task, allowing early cancellation.
 */
@FunctionalInterface
public interface ScheduleHandle {

    /**
     * Cancels the scheduled task. Safe to call multiple times.
     */
    void cancel();
}
