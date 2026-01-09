package dev.objz.commandbridge.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.backends.net.WsClient;
import dev.objz.commandbridge.backends.net.in.ExecuteCommandHandler;
import dev.objz.commandbridge.backends.net.in.RegistrationHandler;
import dev.objz.commandbridge.backends.platform.PathsUtil;
import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.backends.platform.bootstrap.VelocityMain;
import dev.objz.commandbridge.backends.platform.cmd.CommandExecutor;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;

import java.nio.file.Path;

public final class Adapter implements PlatformAdapter {
	private WsClient client;
	private BackendsConfig cfg;
	private Path dataDir;
	private ProxyServer proxy;
	private Object pluginInstance;
	private VelocityExecutor commandExecutor;

	@Override
	public void load(PlatformEnv env, Object plugin) throws Exception {
		if (!(plugin instanceof VelocityMain)) {
			throw new IllegalArgumentException("Plugin must be instance of VelocityMain");
		}

		VelocityMain bootstrap = (VelocityMain) plugin;
		this.proxy = bootstrap.getProxy();
		this.pluginInstance = bootstrap.getPluginInstance();

		this.dataDir = PathsUtil.normalizeDataDir(env.dataDir());
		var cfgMgr = new ConfigManager(dataDir, env.configName());
		boolean ok = cfgMgr.load(BackendsConfig.class);
		this.cfg = cfgMgr.current(BackendsConfig.class);
		if (!ok)
			Log.error("Could not load BackendsConfig");

		if (cfg != null) {
			Log.setDebug(cfg.debug());
			Log.info("Debug mode is " + (cfg.debug() ? "enabled" : "disabled"));
		}

		this.commandExecutor = new VelocityExecutor(proxy, this.pluginInstance);
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
			if (proxy == null)
				throw new IllegalStateException(
						"ProxyServer not initialized (load() was not called or failed)");
			this.commandExecutor = new VelocityExecutor(proxy, pluginInstance);
		}

		this.client = new WsClient(cfg, dataDir);

		client.setLocation(Location.VELOCITY);

		client.start();

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
			Log.info("Backend (Velocity) stopped");
		}
	}

	@Override
	public CommandExecutor getCommandExecutor() {
		return commandExecutor;
	}
}
