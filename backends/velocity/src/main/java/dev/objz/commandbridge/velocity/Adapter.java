package dev.objz.commandbridge.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;

import dev.objz.commandbridge.backends.net.WsClient;
import dev.objz.commandbridge.backends.net.in.ExecuteCommandHandler;
import dev.objz.commandbridge.backends.net.in.RegistrationHandler;
import dev.objz.commandbridge.backends.platform.PathsUtil;
import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.backends.platform.bootstrap.VelocityMain;
import dev.objz.commandbridge.backends.platform.cmd.ClientCommands;
import dev.objz.commandbridge.backends.platform.cmd.CommandExecutor;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;

import java.nio.file.Path;
import java.time.Duration;

public final class Adapter implements PlatformAdapter {
	private WsClient client;
	private BackendsConfig cfg;
	private Path dataDir;
	private ProxyServer proxy;
	private Object pluginInstance;
	private VelocityExecutor commandExecutor;
	private boolean configOk = true;

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
		if (!ok || cfg == null) {
			configOk = false;
			return;
		}
		if (cfg != null) {
			Log.setDebug(cfg.debug());
			Log.info("Debug mode is " + (cfg.debug() ? "enabled" : "disabled"));
		}

		this.commandExecutor = new VelocityExecutor(proxy, this.pluginInstance);
	}

	@Override
	public void start(PlatformEnv env) throws Exception {
		if (!configOk) {
			return;
		}
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

		this.client = new WsClient(cfg, dataDir, this);

		client.setLocation(Location.VELOCITY);

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
			Log.info("Backend (Velocity) stopped");
		}
	}

	@Override
	public CommandExecutor getCommandExecutor() {
		return commandExecutor;
	}

	@Override
	public Object runSchedule(Runnable task, Duration timeout, Duration interval) {
		return new TimeoutTask(task, timeout.toMillis(), interval).start();
	}

	@Override
	public void cancelSchedule(Object task) {
		if (task instanceof TimeoutTask t) {
			t.cancel();
		}
	}

	private class TimeoutTask implements Runnable {
		private final Runnable delegate;
		private final long timeoutMillis;
		private final Duration interval;
		private final long startTime;
		private ScheduledTask scheduledTask;

		public TimeoutTask(Runnable delegate, long timeoutMillis, Duration interval) {
			this.delegate = delegate;
			this.timeoutMillis = timeoutMillis;
			this.interval = interval;
			this.startTime = System.currentTimeMillis();
		}

		public TimeoutTask start() {
			this.scheduledTask = proxy.getScheduler()
					.buildTask(pluginInstance, this)
					.delay(Duration.ZERO)
					.repeat(interval)
					.schedule();
			return this;
		}

		@Override
		public void run() {
			if (System.currentTimeMillis() - startTime > timeoutMillis) {
				cancel();
				return;
			}
			delegate.run();
		}

		public void cancel() {
			if (scheduledTask != null) {
				scheduledTask.cancel();
			}
		}
	}
}
