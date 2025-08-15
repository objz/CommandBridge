package dev.objz.commandbridge.backends.bukkit;

import org.bukkit.plugin.java.JavaPlugin;

import dev.objz.commandbridge.backends.PlatformInterface;
import dev.objz.commandbridge.backends.ws.WsClient;
import dev.objz.commandbridge.main.config.ConfigManager;
import dev.objz.commandbridge.main.config.model.BackendsConfig;
import dev.objz.commandbridge.main.logging.Log;

import java.nio.file.Path;
import java.util.Locale;

public final class Main implements PlatformInterface {
	private final JavaPlugin plugin;
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

		var cfgMgr = new ConfigManager(lowerCaseDataDir);
		BackendsConfig cfg = cfgMgr.load(BackendsConfig.class);
		Log.setDebug(cfg.debug());
		Log.debug("Debug mode is " + (cfg.debug() ? "enabled" : "disabled"));
		Log.info("Backend running on Bukkit");
		client = new WsClient(cfg);
		client.start();
	}

	@Override
	public void disable() {
		try {
			if (client != null)
				client.close();
		} catch (Exception ignored) {
		}
		Log.info("Backend (Bukkit) stopped");
	}
}
