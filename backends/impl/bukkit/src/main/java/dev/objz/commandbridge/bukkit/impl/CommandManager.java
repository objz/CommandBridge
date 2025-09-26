package dev.objz.commandbridge.bukkit.impl;

import java.util.Objects;

import org.bukkit.plugin.java.JavaPlugin;

import dev.jorel.commandapi.CommandAPICommand;
import dev.objz.commandbridge.backends.PlatformRegistry;
import dev.objz.commandbridge.main.proto.cmd.CommandStub;

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
		CommandAPICommand cmd = new CommandAPICommand(stub.name());

		var aliases = stub.aliases();
		if (aliases != null && !aliases.isEmpty()) {
			cmd.withAliases(aliases.toArray(String[]::new));
		}

		

		cmd.register();

	}

	@Override
	protected void doUnregisterAll() throws Exception {
		// TODO
	}
}
