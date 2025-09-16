package dev.objz.commandbridge.backends.bukkit.cmd;

import dev.objz.commandbridge.backends.api.AbstractCommandRegistry;
import dev.objz.commandbridge.main.proto.cmd.CommandStub;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class CommandRegistry extends AbstractCommandRegistry {
	private final JavaPlugin plugin;

	public CommandRegistry(JavaPlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
	}

	@Override
	protected String platformName() {
		return "Bukkit";
	}

	@Override
	protected void doRegister(CommandStub stub) throws Exception {
		// TODO 
		
		// NOTE: base class will add to 'installed' on success.
		// Throw an exception to count as a failure.
	}

	@Override
	protected void doUnregisterAll() throws Exception {
		// TODO
	}
}
