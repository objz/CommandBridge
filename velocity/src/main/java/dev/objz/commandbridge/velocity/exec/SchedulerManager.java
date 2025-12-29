package dev.objz.commandbridge.velocity.exec;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import dev.objz.commandbridge.logging.Log;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class SchedulerManager {

	private final ProxyServer proxy;
	private final Object plugin;

	public SchedulerManager(ProxyServer proxy, Object plugin) {
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

	public ScheduledTask scheduleRepeating(Runnable task, Duration initialDelay, Duration period) {
		if (task == null) {
			return null;
		}

		Duration effectiveDelay = (initialDelay != null && !initialDelay.isNegative())
				? initialDelay
				: Duration.ZERO;
		Duration effectivePeriod = (period != null && !period.isZero() && !period.isNegative())
				? period
				: Duration.ofSeconds(1);

		Scheduler.TaskBuilder builder = proxy.getScheduler().buildTask(plugin, () -> {
			try {
				task.run();
			} catch (Exception e) {
				Log.error(e, "Repeating task execution failed");
			}
		});

		return builder
				.delay(effectiveDelay.toMillis(), TimeUnit.MILLISECONDS)
				.repeat(effectivePeriod.toMillis(), TimeUnit.MILLISECONDS)
				.schedule();
	}

	public void scheduleAsync(Runnable task) {
		if (task == null) {
			return;
		}

		proxy.getScheduler().buildTask(plugin, () -> {
			try {
				task.run();
			} catch (Exception e) {
				Log.error(e, "Async task execution failed");
			}
		}).schedule();
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
