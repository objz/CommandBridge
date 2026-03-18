package dev.objz.commandbridge.bukkit;

import dev.objz.commandbridge.backends.net.client.BackendClient;
import dev.objz.commandbridge.backends.net.client.RedisClient;
import dev.objz.commandbridge.backends.net.client.WsClient;
import dev.objz.commandbridge.backends.net.in.DumpRequestHandler;
import dev.objz.commandbridge.backends.net.in.ExecuteCommandHandler;
import dev.objz.commandbridge.backends.net.in.RegistrationHandler;
import dev.objz.commandbridge.backends.net.out.ctx.PlayerListContext;
import dev.objz.commandbridge.backends.net.out.ctx.PlayerUpdateContext;
import dev.objz.commandbridge.backends.platform.PathsUtil;
import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.backends.platform.ScheduleHandle;
import dev.objz.commandbridge.backends.platform.cmd.ClientCommands;
import dev.objz.commandbridge.backends.platform.cmd.CommandExecutor;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.config.model.EndpointType;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class Adapter implements PlatformAdapter<JavaPlugin> {
    private BackendClient client;
    private BackendsConfig cfg;
    private Path dataDir;
    private JavaPlugin plugin;
    private BukkitExecutor commandExecutor;
    private boolean configOk = true;

    @Override
    public void load(PlatformEnv env, JavaPlugin plugin) throws Exception {
        this.plugin = Objects.requireNonNull(plugin);
        this.dataDir = PathsUtil.normalizeDataDir(env.dataDir());
        var cfgMgr = new ConfigManager<>(dataDir, BackendsConfig.class);
        boolean ok = cfgMgr.load();
        this.cfg = cfgMgr.current();
        if (!ok || cfg == null) {
            configOk = false;
            return;
        }

        if (cfg != null) {
            Log.setDebug(cfg.debug());
            Log.info("Debug mode is {}", cfg.debug() ? "enabled" : "disabled");
        }

        this.commandExecutor = new BukkitExecutor(this.plugin);
    }

    @Override
    public void start(PlatformEnv env) throws Exception {
        if (!configOk) {
            return;
        }
        if (this.dataDir == null)
            this.dataDir = PathsUtil.normalizeDataDir(env.dataDir());
        if (this.cfg == null) {
            var cfgMgr = new ConfigManager<>(dataDir, BackendsConfig.class);
            if (!cfgMgr.load()) {
                Log.error("Could not load BackendsConfig");
                return;
            }
            this.cfg = cfgMgr.current();
            Log.setDebug(cfg.debug());
        }

        if (this.commandExecutor == null) {
            this.commandExecutor = new BukkitExecutor(plugin);
        }

        Log.installThreadMarshalling(
                Bukkit::isPrimaryThread,
                task -> Bukkit.getScheduler().runTask(plugin, task));

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

        RegistrationHandler registrationHandler = new RegistrationHandler(client);
        client.inboundRouter().register(MessageType.REGISTER_COMMANDS, registrationHandler);
        client.inboundRouter().register(MessageType.EXECUTE_COMMAND,
                new ExecuteCommandHandler(commandExecutor));
        client.inboundRouter().register(MessageType.DUMP_REQUEST,
                new DumpRequestHandler(
                        () -> cfg,
                        registrationHandler::snapshotRegisteredCommands,
                        () -> getOnlinePlayerIds().size(),
                        client::serverId,
                        Location.BACKEND));

        client.onAuthenticated(this::sendPlayerList);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(), plugin);
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

    @Override
    public CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    @Override
    public ScheduleHandle runSchedule(Runnable task, Duration timeout, Duration interval) {
        long intervalTicks = interval.toMillis() / 50;
        long timeoutMillis = timeout.toMillis();
        return new TimeoutTask(task, timeoutMillis, intervalTicks).start();
    }

    @Override
    public Set<UUID> getOnlinePlayerIds() {
        return Bukkit.getOnlinePlayers().stream()
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

    private void sendPlayerJoin(java.util.UUID playerUuid) {
        if (client == null) return;
        try {
            client.outboundRouter().send(MessageType.PLAYER_JOIN,
                    new PlayerUpdateContext(playerUuid));
        } catch (Exception e) {
            Log.debug("Failed to send player join: {}", e.getMessage());
        }
    }

    private void sendPlayerLeave(java.util.UUID playerUuid) {
        if (client == null) return;
        try {
            client.outboundRouter().send(MessageType.PLAYER_LEAVE,
                    new PlayerUpdateContext(playerUuid));
        } catch (Exception e) {
            Log.debug("Failed to send player leave: {}", e.getMessage());
        }
    }

    private class PlayerListener implements Listener {
        @EventHandler
        public void onJoin(PlayerJoinEvent event) {
            sendPlayerJoin(event.getPlayer().getUniqueId());
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            // Delay slightly so the quitting player is removed from online list
            java.util.UUID uuid = event.getPlayer().getUniqueId();
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> sendPlayerLeave(uuid), 1L);
        }
    }

    private class TimeoutTask implements Runnable, ScheduleHandle {
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
