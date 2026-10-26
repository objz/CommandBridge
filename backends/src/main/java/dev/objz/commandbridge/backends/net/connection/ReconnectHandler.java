package dev.objz.commandbridge.backends.net.connection;

import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.backends.platform.ScheduleHandle;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.logging.Log;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ReconnectHandler {
    private final BackendsConfig cfg;
    private final PlatformAdapter<?> adapter;
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    private volatile ScheduleHandle reconnectionTask;
    private volatile long deadline;
    private final Runnable reconnectCallback;

    public ReconnectHandler(BackendsConfig cfg, PlatformAdapter<?> adapter, Runnable reconnectCallback) {
        this.cfg = cfg;
        this.adapter = adapter;
        this.reconnectCallback = reconnectCallback;
    }

    public synchronized void scheduleReconnect() {
        // a platform task can end itself when its time is up without running the last attempt,
        // which would leave this stuck "in progress" for good; past the window, start over
        if (reconnectionTask != null && System.currentTimeMillis() >= deadline) {
            Log.debug("The last reconnect ran out without a result, starting a new one");
            stopReconnect();
        }

        if (isReconnecting.get()) {
            Log.debug("Reconnect already in progress, skipping");
            return;
        }

        if (reconnectionTask != null) {
            Log.debug("Reconnect task already scheduled, skipping");
            return;
        }

        isReconnecting.set(true);

        Duration totalTimeout = Duration.ofSeconds(cfg.timeouts().reconnectTimeout());
        Duration interval = Duration.ofSeconds(cfg.timeouts().reconnectInterval());

        Log.warn("Scheduling automatic reconnection (timeout: {}s, interval: {}s)",
                totalTimeout.getSeconds(),
                interval.getSeconds());

        long startTime = System.currentTimeMillis();
        deadline = startTime + totalTimeout.toMillis() + interval.toMillis();

        Runnable task = () -> {
            if (!isReconnecting.get()) {
                stopReconnect();
                return;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            boolean isLastAttempt = elapsed >= totalTimeout.toMillis();

            try {
                Log.info("Attempting to reconnect");
                reconnectCallback.run();

            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                String errorMsg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();

                if (isLastAttempt) {
                    Log.error("All reconnection attempts failed after {}s", totalTimeout.getSeconds());
                    isReconnecting.set(false);
                    stopReconnect();
                } else {
                    Log.warn("Reconnection attempt failed: {}", errorMsg);
                }
            }
        };

        this.reconnectionTask = adapter.runSchedule(task, totalTimeout, interval);
    }

    public synchronized void onReconnectSuccess() {
        if (isReconnecting.getAndSet(false)) {
            stopReconnect();
            Log.success("Reconnected successfully");
        }
    }

    public synchronized void stopReconnect() {
        if (reconnectionTask != null) {
            reconnectionTask.cancel();
            reconnectionTask = null;
        }
        isReconnecting.set(false);
    }

    public boolean isReconnecting() {
        return isReconnecting.get();
    }

    public synchronized void shutdown() {
        isReconnecting.set(false);
        stopReconnect();
    }
}
