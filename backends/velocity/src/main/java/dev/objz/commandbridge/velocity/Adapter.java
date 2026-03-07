package dev.objz.commandbridge.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;

import dev.objz.commandbridge.backends.net.client.BackendClient;
import dev.objz.commandbridge.backends.net.client.RedisClient;
import dev.objz.commandbridge.backends.net.client.WsClient;
import dev.objz.commandbridge.backends.net.in.ExecuteCommandHandler;
import dev.objz.commandbridge.backends.net.in.RegistrationHandler;
import dev.objz.commandbridge.backends.net.in.ResolveUuidHandler;
import dev.objz.commandbridge.backends.net.out.ctx.PlayerListContext;
import dev.objz.commandbridge.backends.net.out.ctx.PlayerUpdateContext;
import dev.objz.commandbridge.backends.platform.PathsUtil;
import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.backends.platform.bootstrap.VelocityMain;
import dev.objz.commandbridge.backends.platform.cmd.ClientCommands;
import dev.objz.commandbridge.backends.platform.cmd.CommandExecutor;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.config.model.EndpointType;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class Adapter implements PlatformAdapter {
    private BackendClient client;
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

        this.client = cfg.endpointType() == EndpointType.REDIS
                ? new RedisClient(cfg, dataDir, this)
                : new WsClient(cfg, dataDir, this);

        client.setLocation(Location.VELOCITY);

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
        client.inboundRouter().register(MessageType.RESOLVE_UUID,
                new ResolveUuidHandler(name -> proxy.getPlayer(name)
                        .map(Player::getUniqueId).orElse(null)));

        client.onAuthenticated(this::sendPlayerList);
        proxy.getEventManager().register(pluginInstance, new PlayerListener());
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

    @Override
    public Set<UUID> getOnlinePlayerIds() {
        return proxy.getAllPlayers().stream()
                .map(Player::getUniqueId)
                .collect(Collectors.toSet());
    }

    private void sendPlayerList() {
        if (client == null) return;
        try {
            client.outboundRouter().send(MessageType.PLAYER_LIST,
                    new PlayerListContext(getOnlinePlayerIds()));
        } catch (Exception e) {
            Log.debug("Failed to send player list: {}", e.getMessage());
        }
    }

    private void sendPlayerJoin(UUID playerUuid) {
        if (client == null) return;
        try {
            client.outboundRouter().send(MessageType.PLAYER_JOIN,
                    new PlayerUpdateContext(playerUuid));
        } catch (Exception e) {
            Log.debug("Failed to send player join: {}", e.getMessage());
        }
    }

    private void sendPlayerLeave(UUID playerUuid) {
        if (client == null) return;
        try {
            client.outboundRouter().send(MessageType.PLAYER_LEAVE,
                    new PlayerUpdateContext(playerUuid));
        } catch (Exception e) {
            Log.debug("Failed to send player leave: {}", e.getMessage());
        }
    }

    private class PlayerListener {
        @Subscribe
        public void onPostLogin(PostLoginEvent event) {
            sendPlayerJoin(event.getPlayer().getUniqueId());
        }

        @Subscribe
        public void onDisconnect(DisconnectEvent event) {
            UUID uuid = event.getPlayer().getUniqueId();
            proxy.getScheduler().buildTask(pluginInstance, () -> sendPlayerLeave(uuid))
                    .delay(50, TimeUnit.MILLISECONDS)
                    .schedule();
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
