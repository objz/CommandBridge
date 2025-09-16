package dev.objz.commandbridge.backends.folia.cmd;

import dev.objz.commandbridge.backends.PlatformRegistry;
import dev.objz.commandbridge.main.proto.cmd.CommandStub;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class CommandManager extends PlatformRegistry {
	private final JavaPlugin plugin;

	public CommandManager(JavaPlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
	}

	@Override protected String platformName() { return "Folia"; }

	@Override
	protected void doRegister(CommandStub stub) throws Exception {
		// TODO
	}

	@Override
	protected void doUnregisterAll() throws Exception {
		// TODO
	}
}
