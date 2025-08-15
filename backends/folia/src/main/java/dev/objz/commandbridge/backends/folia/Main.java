package dev.objz.commandbridge.backends.folia;

import org.bukkit.plugin.java.JavaPlugin;

import dev.objz.commandbridge.backends.PlatformInterface;
import dev.objz.commandbridge.backends.ws.ClientWebSocket;
import dev.objz.commandbridge.main.config.ConfigManager;
import dev.objz.commandbridge.main.config.model.BackendsConfig;
import dev.objz.commandbridge.main.logging.Log;

import java.nio.file.Path;

public final class Main implements PlatformInterface {
	private final JavaPlugin plugin;
	private ClientWebSocket client;

	public Main(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void enable() {
		Path dataDir = plugin.getDataFolder().toPath();
		var cfgMgr = new ConfigManager(dataDir);
		BackendsConfig cfg = cfgMgr.load(BackendsConfig.class);
		Log.setDebug(cfg.debug());
		Log.info("Backend running on Folia");
		client = new ClientWebSocket(cfg);
		client.start();
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
}
