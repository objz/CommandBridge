package dev.objz.commandbridge.paper;

import dev.objz.commandbridge.backends.net.WsClient;
import dev.objz.commandbridge.backends.net.in.RegistrationHandler;
import dev.objz.commandbridge.backends.platform.PathsUtil;
import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.proto.MessageType;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class Adapter implements PlatformAdapter {
	private WsClient client;
	private BackendsConfig cfg;
	private Path dataDir;
	private JavaPlugin plugin;

	@Override
	public void load(PlatformEnv env, JavaPlugin plugin) throws Exception {
		this.plugin = plugin;
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

		Log.installThreadMarshalling(
				() -> Bukkit.isPrimaryThread(),
				task -> Bukkit.getScheduler().runTask((JavaPlugin) plugin, task));

		this.client = new WsClient(cfg, dataDir);
		client.inboundRouter().register(MessageType.REGISTER_COMMANDS, new RegistrationHandler(client));
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
