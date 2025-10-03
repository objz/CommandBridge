package dev.objz.commandbridge.bukkit.impl;

import java.util.Objects;

import org.bukkit.plugin.java.JavaPlugin;

import dev.objz.commandbridge.backends.PlatformRegistry;
import dev.objz.commandbridge.proto.cmd.CommandStub;

public final class CommandManager extends PlatformRegistry {
	private final JavaPlugin plugin;

	public CommandManager(JavaPlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
	}

	@Override
	protected String platformName() {
		return "Bukkit";
	}

	@Override
	protected void doRegister(CommandStub stub) throws Exception {
		// TODO
	}

	@Override
	protected void doUnregisterAll() throws Exception {
		// TODO
	}
}
