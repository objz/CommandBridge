package dev.objz.commandbridge.folia;

import dev.objz.commandbridge.backends.net.BackendClient;
import dev.objz.commandbridge.backends.net.RedisClient;
import dev.objz.commandbridge.backends.net.WsClient;
import dev.objz.commandbridge.backends.net.in.ExecuteCommandHandler;
import dev.objz.commandbridge.backends.net.in.RegistrationHandler;
import dev.objz.commandbridge.backends.platform.PathsUtil;
import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.backends.platform.cmd.ClientCommands;
import dev.objz.commandbridge.backends.platform.cmd.CommandExecutor;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.config.model.EndpointType;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.proto.MessageType;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class Adapter implements PlatformAdapter {
    private BackendClient client;
    private BackendsConfig cfg;
    private Path dataDir;
    private JavaPlugin plugin;
    private FoliaExecutor commandExecutor;
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

        this.commandExecutor = new FoliaExecutor(this.plugin);
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
            this.commandExecutor = new FoliaExecutor(plugin);
        }

        this.client = cfg.endpointType() == EndpointType.REDIS
                ? new RedisClient(cfg, dataDir, this)
                : new WsClient(cfg, dataDir, this);

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
            Log.info("Backend (Folia) stopped");
        }
    }

    @Override
    public CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    @Override
    public Object runSchedule(Runnable task, Duration timeout, Duration interval) {
        return new TimeoutTask(task, timeout.toMillis(), interval.toMillis()).start();
    }

    @Override
    public void cancelSchedule(Object task) {
        if (task instanceof TimeoutTask t) {
            t.cancel();
        }
    }

    private class TimeoutTask {
        private final Runnable delegate;
        private final long timeoutMillis;
        private final long intervalMillis;
        private final long startTime;
        private ScheduledTask scheduledTask;

        public TimeoutTask(Runnable delegate, long timeoutMillis, long intervalMillis) {
            this.delegate = delegate;
            this.timeoutMillis = timeoutMillis;
            this.intervalMillis = intervalMillis;
            this.startTime = System.currentTimeMillis();
        }

        public TimeoutTask start() {
            this.scheduledTask = Bukkit.getAsyncScheduler().runAtFixedRate(
                    plugin,
                    (t) -> run(),
                    0, // Initial delay
                    intervalMillis,
                    TimeUnit.MILLISECONDS);
            return this;
        }

        private void run() {
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
