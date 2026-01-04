package dev.objz.commandbridge.velocity.dispatch;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import dev.objz.commandbridge.logging.Log;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class ScheduleManager {

	private final ProxyServer proxy;
	private final Object plugin;

	public ScheduleManager(ProxyServer proxy, Object plugin) {
		this.proxy = proxy;
		this.plugin = plugin;
	}

	public void schedule(Runnable task, Duration delay) {
		if (task == null) {
			return;
		}

		if (delay == null || delay.isZero() || delay.isNegative()) {
			try {
				task.run();
			} catch (Exception e) {
				Log.error(e, "Scheduled task execution failed");
			}
			return;
		}

		Scheduler.TaskBuilder builder = proxy.getScheduler().buildTask(plugin, () -> {
			try {
				task.run();
			} catch (Exception e) {
				Log.error(e, "Delayed task execution failed");
			}
		});
		builder.delay(delay.toMillis(), TimeUnit.MILLISECONDS).schedule();
	}

	public void cancel(ScheduledTask task) {
		if (task != null) {
			try {
				task.cancel();
			} catch (Exception e) {
				Log.debug("Failed to cancel task:  {}", e.getMessage());
			}
		}
	}
}
