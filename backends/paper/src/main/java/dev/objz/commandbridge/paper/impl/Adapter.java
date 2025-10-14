package dev.objz.commandbridge.paper.impl;

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
	private BackendsConfig cfg;
	private Path dataDir;

	@Override
	public void load(PlatformEnv env) throws Exception {
		this.dataDir = PathsUtil.normalizeDataDir(env.dataDir());
		var cfgMgr = new ConfigManager(dataDir);
		boolean ok = cfgMgr.load(BackendsConfig.class);
		this.cfg = cfgMgr.current(BackendsConfig.class);
		if (!ok)
			Log.error("Could not load BackendsConfig");

		if (cfg != null) {
			Log.setDebug(cfg.debug());
			Log.info("Debug mode is " + (cfg.debug() ? "enabled" : "disabled"));
		}
	}

	@Override
	public void start(PlatformEnv env) throws Exception {
		if (this.dataDir == null)
			this.dataDir = PathsUtil.normalizeDataDir(env.dataDir());
		if (this.cfg == null) {
			var cfgMgr = new ConfigManager(dataDir);
			if (!cfgMgr.load(BackendsConfig.class)) {
				Log.error("Could not load BackendsConfig");
				return;
			}
			this.cfg = cfgMgr.current(BackendsConfig.class);
			Log.setDebug(cfg.debug());
		}

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
			Log.info("Backend (Paper) stopped");
		}
	}
}
