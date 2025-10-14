package dev.objz.commandbridge.bukkit.impl;

import dev.objz.commandbridge.backends.platform.PathsUtil;
import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.backends.ws.WsClient;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.logging.Log;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class Adapter implements PlatformAdapter {
	private WsClient client;

	@Override
	public void start(PlatformEnv env) throws Exception {
		Path dataDir = PathsUtil.normalizeDataDir(env.dataDir());

		var cfgMgr = new ConfigManager(dataDir);
		boolean ok = cfgMgr.load(BackendsConfig.class);
		BackendsConfig cfg = cfgMgr.current(BackendsConfig.class);
		if (!ok) {
			Log.error("Could not load BackendsConfig");
			return;
		}

		Log.setDebug(cfg.debug());
		Log.info("Debug mode is " + (cfg.debug() ? "enabled" : "disabled"));
		Log.info("Backend running on Bukkit");

		Plugin plugin = Bukkit.getPluginManager().getPlugin("CommandBridge");
		Log.installThreadMarshalling(
				() -> Bukkit.isPrimaryThread(),
				task -> Bukkit.getScheduler().runTask((JavaPlugin) plugin, task));

		this.client = new WsClient(cfg, dataDir);
		client.start();
	}

	@Override
	public void stop() throws Exception {
		try {
			if (client != null)
				client.close();
		} finally {
			Log.info("Backend (Bukkit) stopped");
		}
	}
}
