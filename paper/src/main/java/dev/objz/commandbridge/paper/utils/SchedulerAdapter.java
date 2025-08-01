package dev.objz.commandbridge.paper.utils;

import org.bukkit.plugin.java.JavaPlugin;

import com.cjcrafter.foliascheduler.FoliaCompatibility;
import com.cjcrafter.foliascheduler.ServerImplementation;

public class SchedulerAdapter {
	private final ServerImplementation scheduler;

	public SchedulerAdapter(JavaPlugin plugin) {
		this.scheduler = new FoliaCompatibility(plugin).getServerImplementation();
	}

	public void run(Runnable task) {
		// if (isFolia()) {
			// Bukkit.getServer().getScheduler().runTask(plugin, (@NotNull Runnable) task);
		// } else {
			// Bukkit.getScheduler().runTask(plugin, task);
		// }
		scheduler.global().run(task);
	}

	public void runLater(Runnable task, long delayTicks) {
		// if (isFolia()) {
			// Bukkit.getServer().getScheduler().runTaskLater(plugin, (@NotNull Runnable) task, delayTicks);
		// } else {
			// Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
		// }
		scheduler.global().runDelayed(task, delayTicks);
	}

	public static boolean isFolia() {
		try {
			Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}
