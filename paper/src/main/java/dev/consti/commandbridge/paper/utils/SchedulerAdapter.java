package dev.consti.commandbridge.paper.utils;

import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public class SchedulerAdapter {
    private final JavaPlugin plugin;

    public SchedulerAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void run(Runnable task) {
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, task);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runLater(Runnable task, long delayTicks) {
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, (@NotNull Consumer<ScheduledTask>) task, delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
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
