package dev.objz.commandbridge.folia;

import dev.objz.commandbridge.backends.net.WsClient;
import dev.objz.commandbridge.backends.net.in.ExecuteCommandHandler;
import dev.objz.commandbridge.backends.net.in.RegistrationHandler;
import dev.objz.commandbridge.backends.platform.PathsUtil;
import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.backends.platform.cmd.ClientCommands;
import dev.objz.commandbridge.backends.platform.cmd.CommandExecutor;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.proto.MessageType;

import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class Adapter implements PlatformAdapter {
	private WsClient client;
	private BackendsConfig cfg;
	private Path dataDir;
	private JavaPlugin plugin;
	private FoliaExecutor commandExecutor;

	@Override
	public void load(PlatformEnv env, Object plugin) throws Exception {
		this.plugin = (JavaPlugin) plugin;
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

		this.commandExecutor = new FoliaExecutor(this.plugin);
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

		if (this.commandExecutor == null) {
			this.commandExecutor = new FoliaExecutor(plugin);
		}

		this.client = new WsClient(cfg, dataDir);

		client.start();

		ClientCommands.register(client);

		client.inboundRouter().register(MessageType.REGISTER_COMMANDS, new RegistrationHandler(client));
		client.inboundRouter().register(MessageType.EXECUTE_COMMAND,
				new ExecuteCommandHandler(commandExecutor));
	}

	@Override
	public void stop() throws Exception {
		try {
			if (client != null)
				client.close();
		} finally {
			Log.info("Backend (Folia) stopped");
		}
	}

	@Override
	public CommandExecutor getCommandExecutor() {
		return commandExecutor;
	}
}
