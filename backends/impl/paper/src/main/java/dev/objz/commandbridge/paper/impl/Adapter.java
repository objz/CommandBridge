package dev.objz.commandbridge.paper.impl;

import dev.objz.commandbridge.backends.PlatformRegistry;
import dev.objz.commandbridge.backends.ws.WsClient;
import dev.objz.commandbridge.main.config.ConfigManager;
import dev.objz.commandbridge.main.config.model.BackendsConfig;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.platform.PathsUtil;
import dev.objz.commandbridge.platform.PlatformAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class Adapter implements PlatformAdapter {
	private PlatformRegistry registry;
	private WsClient client;

	@Override
	public void start(PlatformEnv env) throws Exception {
		Path dataDir = PathsUtil.normalizeDataDir(env.dataDir());

		Plugin plugin = Bukkit.getPluginManager().getPlugin("CommandBridge");
		this.registry = new CommandManager((JavaPlugin) plugin);

		var cfgMgr = new ConfigManager(dataDir);
		boolean ok = cfgMgr.load(BackendsConfig.class);
		BackendsConfig cfg = cfgMgr.current(BackendsConfig.class);
		if (!ok) {
			Log.error("Could not load BackendsConfig");
			return;
		}

		Log.setDebug(cfg.debug());
		Log.debug("Debug mode is " + (cfg.debug() ? "enabled" : "disabled"));
		Log.info("Backend running on Paper");

		this.client = new WsClient(cfg, () -> registry, dataDir);
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
