package dev.objz.commandbridge.backends.bukkit;

import org.bukkit.plugin.java.JavaPlugin;

import dev.objz.commandbridge.backends.PlatformInterface;

public final class Main implements PlatformInterface {
	private final JavaPlugin plugin;

	public Main(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void enable() {
		plugin.getLogger().info("Hello from bukkit");
	}

	@Override
	public void disable() {
		plugin.getLogger().info("Bye from bukkit");
	}
}
