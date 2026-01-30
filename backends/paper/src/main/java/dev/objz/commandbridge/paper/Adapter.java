package dev.objz.commandbridge.paper;

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

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.nio.file.Path;
import java.time.Duration;

public final class Adapter implements PlatformAdapter {
	private WsClient client;
	private BackendsConfig cfg;
	private Path dataDir;
	private JavaPlugin plugin;
	private PaperExecutor commandExecutor;
	private boolean configOk = true;

	@Override
	public void load(PlatformEnv env, Object plugin) throws Exception {
		this.plugin = (JavaPlugin) plugin;
		this.dataDir = PathsUtil.normalizeDataDir(env.dataDir());
		var cfgMgr = new ConfigManager(dataDir);
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

		this.commandExecutor = new PaperExecutor(this.plugin);
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
			this.commandExecutor = new PaperExecutor(plugin);
		}

		Log.installThreadMarshalling(
				() -> Bukkit.isPrimaryThread(),
				task -> Bukkit.getScheduler().runTask(plugin, task));

		this.client = new WsClient(cfg, dataDir, this);

		try {
			client.start();
		} catch (Exception e) {
			// Connection failed, schedule automatic reconnection
			client.scheduleReconnection();
		}

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
			Log.info("Backend (Paper) stopped");
		}
	}

	@Override
	public CommandExecutor getCommandExecutor() {
		return commandExecutor;
	}

	@Override
	public Object runSchedule(Runnable task, Duration timeout, Duration interval) {
		long intervalTicks = interval.toMillis() / 50;
		long timeoutMillis = timeout.toMillis();
		return new TimeoutTask(task, timeoutMillis, intervalTicks).start();
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
		private final long intervalTicks;
		private final long startTime;
		private BukkitTask bukkitTask;

		public TimeoutTask(Runnable delegate, long timeoutMillis, long intervalTicks) {
			this.delegate = delegate;
			this.timeoutMillis = timeoutMillis;
			this.intervalTicks = intervalTicks;
			this.startTime = System.currentTimeMillis();
		}

		public TimeoutTask start() {
			this.bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this, 0L,
					intervalTicks);
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
			if (bukkitTask != null && !bukkitTask.isCancelled()) {
				bukkitTask.cancel();
			}
		}
	}
}
