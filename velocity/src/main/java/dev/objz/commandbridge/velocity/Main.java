package dev.objz.commandbridge.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.api.CommandBridgeProvider;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.EndpointType;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.InNode;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.security.AuthService;
import dev.objz.commandbridge.security.SecretLoader;
import dev.objz.commandbridge.security.TlsResolver;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.platform.PlatformFeatureKeys;
import dev.objz.commandbridge.scripting.platform.PlatformFeatureSet;
import dev.objz.commandbridge.scripting.platform.PlatformFeatures;
import dev.objz.commandbridge.util.BuildMeta;
import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.util.ModrinthAPI;
import dev.objz.commandbridge.velocity.cli.CBCommand;
import dev.objz.commandbridge.velocity.dispatch.CommandEntry;
import dev.objz.commandbridge.velocity.api.VelocityCommandBridgeImpl;
import dev.objz.commandbridge.velocity.api.VelocityPluginMessageHandler;
import dev.objz.commandbridge.velocity.api.PluginMessageQueue;
import dev.objz.commandbridge.velocity.cmd.bridge.framework.ArgumentBridge;
import dev.objz.commandbridge.velocity.cmd.bridge.packetevents.PacketEventsArgumentBridge;
import dev.objz.commandbridge.velocity.cmd.bridge.types.OfflinePlayerArgumentType;
import dev.objz.commandbridge.velocity.net.EndpointServer;
import dev.objz.commandbridge.velocity.net.RedisServer;
import dev.objz.commandbridge.velocity.net.WsServer;
import dev.objz.commandbridge.velocity.net.in.AuthHandler;
import dev.objz.commandbridge.velocity.net.in.ExecuteCommandHandler;
import dev.objz.commandbridge.velocity.net.in.InvokedCommandHandler;
import dev.objz.commandbridge.velocity.net.in.PlayerListHandler;
import dev.objz.commandbridge.velocity.net.in.PlayerUpdateHandler;
import dev.objz.commandbridge.velocity.net.out.ExecuteCommandRequest;
import dev.objz.commandbridge.velocity.net.out.DumpRequest;
import dev.objz.commandbridge.velocity.net.out.PingRequest;
import dev.objz.commandbridge.velocity.net.out.RegistrationRequest;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.net.out.ResolveUuidRequest;
import dev.objz.commandbridge.velocity.util.PlayerTracker;
import dev.objz.commandbridge.velocity.util.UserCache;
import dev.objz.commandbridge.velocity.ui.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import org.slf4j.Logger;
import org.bstats.velocity.Metrics;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Plugin(id = "commandbridge", name = "CommandBridge", version = "unknown", url = "https://cb.objz.dev", description = "I did it!", authors = {
        "objz" }, dependencies = { @Dependency(id = "commandapi"),
            @Dependency(id = "papiproxybridge", optional = true),
            @Dependency(id = "packetevents", optional = true) })
public final class Main {

    private final ProxyServer proxy;
    private final Path dataDir;
    private final Object pluginInstance;
    private final Logger velocityLogger;
    private final Metrics.Factory metrics;

    private ConfigManager<VelocityConfig> configManager;
    private EndpointServer endpointServer;
    private RegistrationManager registrations;
    private InNode inNode;
    private OutNode outNode;
    private VelocityConfig cfg;
    private SessionHub sessions;
    private PlayerTracker playerTracker;
    private PluginMessageQueue pluginMessageQueue;
    private UserCache userCache;
    private AuthHandler authHandler;
    private CBCommand command;
    private ScriptManager scriptManager;
    private CommandEntry commandEntry;
    private dev.objz.commandbridge.lifecycle.BackendLifecycle backendBootstrap;
    private VelocityCommandBridgeImpl api;
    private ArgumentBridge argumentBridge;
    private PlatformFeatures platformFeatures;
    private boolean legacyDetected;
    private volatile String latestVersion;



    @Inject
    public Main(ProxyServer proxy, Logger velocityLogger, @DataDirectory Path dataDir, Metrics.Factory metrics) {
        this.proxy = proxy;
        this.dataDir = dataDir;
        this.metrics = metrics;
        this.pluginInstance = this;
        this.velocityLogger = velocityLogger;
        Log.install(velocityLogger);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e) {
        int pluginID = 22008;
        metrics.make(this, pluginID);
        Log.info("Initializing CommandBridge");

        copyExampleScript();
        checkLegacyInstallation();

        configManager = new ConfigManager<>(dataDir, VelocityConfig.class);
        boolean ok = configManager.load();
        cfg = configManager.current();
        if (!ok || cfg == null) {
            return;
        }
        Log.setDebug(cfg.debug());

        if (cfg.actAsClient()) {
            loadClientMode();
            return;
        }

        boolean hasPapi = proxy.getPluginManager().getPlugin("papiproxybridge").isPresent();
        boolean hasPacketEvents = proxy.getPluginManager().getPlugin("packetevents").isPresent();

        var featuresBuilder = PlatformFeatureSet.builder();
        if (hasPapi) {
            Log.success("Hooked into PapiProxyBridge. PlaceholderAPI is now enabled");
            featuresBuilder.add(Location.VELOCITY, PlatformFeatureKeys.PAPI);
        } else {
            Log.warn("PapiProxyBridge not found. PlaceholderAPI will not be used");
        }
        if (hasPacketEvents) {
            featuresBuilder.add(Location.VELOCITY, PlatformFeatureKeys.PACKET_EVENTS);
        } else {
            Log.warn("PacketEvents not found. Some placeholder may not be available on velocity");
        }
        platformFeatures = featuresBuilder.build();

        argumentBridge = new ArgumentBridge(proxy, platformFeatures);
        if (hasPacketEvents) {
            new PacketEventsArgumentBridge(argumentBridge.registry()).install();
        }

        sessions = new SessionHub();
        playerTracker = new PlayerTracker();
        pluginMessageQueue = new PluginMessageQueue();
        sessions.onRemove(session -> {
            playerTracker.remove(session.id());
            if (api != null) {
                api.onServerDisconnected(session);
            }
        });
        inNode = new InNode();
        outNode = new OutNode();
        outNode.setServerId(cfg.serverId());

        if (cfg.endpointType() == EndpointType.REDIS) {
            var redis = cfg.endpoints().redis();
            endpointServer = new RedisServer(
                    redis.host(),
                    redis.port(),
                    redis.username(),
                    redis.password(),
                    sessions,
                    inNode);
        } else {
            var wsCfg = cfg.endpoints().webSocket();
            var tls = TlsResolver.resolveServer(dataDir, cfg.security());
            endpointServer = tls.enabled()
                    ? new WsServer(wsCfg.bindHost(), wsCfg.bindPort(), sessions, inNode,
                            true, tls.context())
                    : new WsServer(wsCfg.bindHost(), wsCfg.bindPort(), sessions, inNode);
        }
        endpointServer.start();

        scriptManager = new ScriptManager(dataDir, platformFeatures);
        scriptManager.loadAll();

        registrations = new RegistrationManager(proxy, sessions, cfg, outNode,
                argumentBridge.registry());

        userCache = new UserCache(proxy, sessions, outNode,
                dataDir.resolve("data").resolve("usercache.json"));

        argumentBridge.registry().register(new OfflinePlayerArgumentType(proxy, userCache));

        proxy.getScheduler().buildTask(pluginInstance, userCache::save)
                .repeat(5, TimeUnit.MINUTES)
                .schedule();

        commandEntry = new CommandEntry(proxy, pluginInstance, scriptManager, sessions, outNode,
                cfg.serverId(), dataDir, playerTracker, userCache, platformFeatures);

        registrations.setCommandEntry(commandEntry);

        registrations.load(scriptManager.enabled());

        api = new VelocityCommandBridgeImpl(sessions, playerTracker, cfg.serverId(), endpointServer, pluginMessageQueue);

        installRoutes();

        command = new CBCommand(
                configManager,
                scriptManager,
                registrations,
                sessions,
                outNode,
                cfg,
                dataDir);
        command.register();

        CommandBridgeProvider.register(api);
        authHandler.onAuthenticated(session -> {
            registrations.onClientAuthenticated(session);
            api.onServerConnected(session);
        });

        checkForUpdate();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent e) {
        Log.info("Stopping CommandBridge");

        if (userCache != null) {
            userCache.save();
        }

        if (backendBootstrap != null) {
            backendBootstrap.disable();
        }

        if (endpointServer != null) {
            endpointServer.stop();
        }

        CommandBridgeProvider.unregister();
    }

    private void installRoutes() {
        var secret = new SecretLoader(dataDir).loadOrCreate();
        var auth = new AuthService(secret);
        authHandler = new AuthHandler(auth, sessions, endpointServer);
        authHandler.register(inNode);

        inNode.register(MessageType.INVOKED_COMMAND, new InvokedCommandHandler(sessions, commandEntry));
        inNode.register(MessageType.EXECUTE_COMMAND_RESULT, new ExecuteCommandHandler(proxy));
        inNode.register(MessageType.PLAYER_LIST, new PlayerListHandler(sessions, playerTracker));
        var playerUpdateHandler = new PlayerUpdateHandler(sessions, playerTracker);
        inNode.register(MessageType.PLAYER_JOIN, playerUpdateHandler);
        inNode.register(MessageType.PLAYER_LEAVE, playerUpdateHandler);
        inNode.register(MessageType.PLUGIN_MESSAGE, new VelocityPluginMessageHandler(api, false));
        inNode.register(MessageType.PLUGIN_MESSAGE_RESPONSE, new VelocityPluginMessageHandler(api, true));

        outNode.setEndpointSendFactory((endpoint, env) -> endpointServer.send(endpoint, env));
        outNode.register(MessageType.REGISTER_COMMANDS, new RegistrationRequest());
        outNode.register(MessageType.PING, new PingRequest());
        outNode.register(MessageType.EXECUTE_COMMAND, new ExecuteCommandRequest());
        outNode.register(MessageType.RESOLVE_UUID, new ResolveUuidRequest());
        outNode.register(MessageType.DUMP_REQUEST, new DumpRequest());
    }

    private void loadClientMode() {
        try {
            String bootstrapClass = "dev.objz.commandbridge.backends.platform.bootstrap.VelocityMain";
            Class<?> clazz = Class.forName(bootstrapClass);

            var ctor = clazz.getConstructor(ProxyServer.class, Logger.class, Path.class, Object.class);
            var instance = ctor.newInstance(proxy, velocityLogger, dataDir, pluginInstance);

            if (!(instance instanceof dev.objz.commandbridge.lifecycle.BackendLifecycle lifecycle)) {
                Log.error("Backend bootstrap does not implement BackendLifecycle");
                return;
            }

            this.backendBootstrap = lifecycle;
            lifecycle.load();
            lifecycle.enable();

            Log.success(true, "CommandBridge running in Client Mode (Backend)");

        } catch (ClassNotFoundException ex) {
            Log.error("Could not find backend bootstrap class. Ensure the 'backends' module is included in your build");
        } catch (Exception ex) {
            Log.error(ex, "Failed to start client mode");
        }
    }

    private void copyExampleScript() {
        Path scriptsDir = dataDir.resolve("scripts");
        Path exampleFile = scriptsDir.resolve("example.yml");
        if (Files.exists(exampleFile)) return;

        try {
            Files.createDirectories(scriptsDir);
            try (InputStream in = getClass().getResourceAsStream("/example.yml")) {
                if (in != null) {
                    Files.copy(in, exampleFile);
                    Log.debug("Created example script at scripts/example.yml");
                }
            }
        } catch (IOException ex) {
            Log.error("Failed to copy example script: {}", ex.getMessage());
        }
    }

    private void checkLegacyInstallation() {
        Path oldFolder = dataDir.getParent().resolve("CommandBridge");
        if (!Files.isDirectory(oldFolder)) return;

        legacyDetected = true;
        Log.warn("Detected old CommandBridge installation at '{}'. Please view the migration guide: https://cb.objz.dev/docs/migration/", oldFolder);
    }

    private void checkForUpdate() {
        proxy.getScheduler().buildTask(pluginInstance, () -> {
            String latest = ModrinthAPI.getLatestVersion("commandbridge");
            if (latest == null) return;

            latestVersion = latest;
            if (compareVersions(latest, BuildMeta.VERSION) > 0) {
                String updateUrl = "https://modrinth.com/plugin/commandbridge/version/" + latest;
                Log.warn("A new version of CommandBridge is available: CommandBridge {} (current: {})", latest, BuildMeta.VERSION);
                Log.warn("Download it at: {}", updateUrl);
            }
        }).schedule();
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();

        if (userCache != null) {
            userCache.cachePlayer(player.getUsername(), player.getUniqueId());
        }

        if (!player.hasPermission("commandbridge.admin")) return;

        boolean hasUpdate = latestVersion != null && compareVersions(latestVersion, BuildMeta.VERSION) > 0;

        if (!legacyDetected && !hasUpdate) return;

        proxy.getScheduler().buildTask(pluginInstance, () -> {
            if (!player.isActive()) return;

            if (legacyDetected) {
                String migrationUrl = "https://cb.objz.dev/docs/migration/";

                player.sendMessage(Component.empty());
                player.sendMessage(MM.parse(
                        "<" + Theme.C_WARN + "><bold>\u26A0 CommandBridge</bold></" + Theme.C_WARN + "> "
                        + "<" + Theme.C_ERROR + ">Old installation detected</" + Theme.C_ERROR + ">"));
                player.sendMessage(MM.parse(
                        "<" + Theme.C_MUTED + ">A legacy </><white>CommandBridge</white>"
                        + "<" + Theme.C_MUTED + "> folder was found in your plugins directory.</" + Theme.C_MUTED + ">"));
                player.sendMessage(MM.parse(
                        "<" + Theme.C_MUTED + ">View the migration guide: </" + Theme.C_MUTED + ">"
                        + "<" + Theme.C_ACCENT + "><underlined>" + migrationUrl + "</underlined></" + Theme.C_ACCENT + ">")
                        .clickEvent(ClickEvent.openUrl(migrationUrl))
                        .hoverEvent(HoverEvent.showText(MM.parse(
                                "<" + Theme.C_MUTED + ">Click to open migration guide</" + Theme.C_MUTED + ">"))));
                player.sendMessage(Component.empty());
            }

            if (hasUpdate) {
                String updateUrl = "https://modrinth.com/plugin/commandbridge/version/" + latestVersion;

                player.sendMessage(Component.empty());
                player.sendMessage(MM.parse(
                        "<" + Theme.C_WARN + "><bold>\u26A0 CommandBridge</bold></" + Theme.C_WARN + "> "
                        + "<" + Theme.C_ACCENT + ">Update available</" + Theme.C_ACCENT + ">"));
                player.sendMessage(MM.parse(
                        "<" + Theme.C_MUTED + ">A new version is available: </>"
                        + "<white>CommandBridge " + latestVersion + "</white>"
                        + "<" + Theme.C_MUTED + "> (current: " + BuildMeta.VERSION + ")</" + Theme.C_MUTED + ">"));
                player.sendMessage(MM.parse(
                        "<" + Theme.C_MUTED + ">Download it here: </" + Theme.C_MUTED + ">"
                        + "<" + Theme.C_ACCENT + "><underlined>" + updateUrl + "</underlined></" + Theme.C_ACCENT + ">")
                        .clickEvent(ClickEvent.openUrl(updateUrl))
                        .hoverEvent(HoverEvent.showText(MM.parse(
                                "<" + Theme.C_MUTED + ">Click to open Modrinth</" + Theme.C_MUTED + ">"))));
                player.sendMessage(Component.empty());
            }
        }).delay(3, TimeUnit.SECONDS).schedule();
    }

    private static int compareVersions(String left, String right) {
        int[] leftParts = parseVersion(left);
        int[] rightParts = parseVersion(right);
        int max = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < max; i++) {
            int leftValue = i < leftParts.length ? leftParts[i] : 0;
            int rightValue = i < rightParts.length ? rightParts[i] : 0;
            if (leftValue != rightValue) {
                return Integer.compare(leftValue, rightValue);
            }
        }
        return 0;
    }

    private static int[] parseVersion(String version) {
        String cleaned = version.replaceAll("[^0-9.]", "");
        if (cleaned.isBlank()) return new int[0];
        String[] parts = cleaned.split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                numbers[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException ex) {
                numbers[i] = 0;
            }
        }
        return numbers;
    }
}
