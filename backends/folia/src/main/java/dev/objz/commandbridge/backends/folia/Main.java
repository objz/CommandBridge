package dev.objz.commandbridge.backends.folia;

import dev.objz.commandbridge.backends.PlatformInterface;
import dev.objz.commandbridge.backends.PlatformRegistry;
import dev.objz.commandbridge.backends.folia.cmd.CommandManager;
import dev.objz.commandbridge.backends.ws.WsClient;
import dev.objz.commandbridge.main.config.ConfigManager;
import dev.objz.commandbridge.main.config.model.BackendsConfig;
import dev.objz.commandbridge.main.logging.Log;
import java.nio.file.Path;
import java.util.Locale;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main implements PlatformInterface {
	private final JavaPlugin plugin;
	private CommandManager registry;
	private WsClient client;

	public Main(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void enable() {
		Path dataDir = plugin.getDataFolder().toPath();
		Path parentDir = dataDir.getParent();
		String lowerCaseName = dataDir.getFileName().toString().toLowerCase(Locale.ROOT);
		Path lowerCaseDataDir = parentDir.resolve(lowerCaseName);

		this.registry = new CommandManager(plugin);

		var cfgMgr = new ConfigManager(lowerCaseDataDir);
		boolean ok = cfgMgr.load(BackendsConfig.class);
		BackendsConfig cfg = cfgMgr.current(BackendsConfig.class);
		if (ok) {
			Log.setDebug(cfg.debug());
			Log.debug("Debug mode is " + (cfg.debug() ? "enabled" : "disabled"));
			Log.info("Backend running on Folia");
			client = new WsClient(cfg, this);
			client.start();
		}
	}

	@Override
	public void disable() {
		try {
			if (client != null)
				client.close();
		} catch (Exception ignored) {
		}
		Log.info("Backend (Folia) stopped");
	}

	@Override
	public PlatformRegistry platformRegistry() {
		return registry;
	}
}
