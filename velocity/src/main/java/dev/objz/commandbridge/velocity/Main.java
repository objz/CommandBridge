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
import dev.objz.commandbridge.velocity.cmd.bridge.framework.ArgumentBridge;
import dev.objz.commandbridge.velocity.cmd.bridge.packetevents.PacketEventsArgumentBridge;
import dev.objz.commandbridge.velocity.net.EndpointServer;
import dev.objz.commandbridge.velocity.net.RedisServer;
import dev.objz.commandbridge.velocity.net.WsServer;
import dev.objz.commandbridge.velocity.net.in.AuthHandler;
import dev.objz.commandbridge.velocity.net.in.ExecuteCommandHandler;
import dev.objz.commandbridge.velocity.net.in.InvokedCommandHandler;
import dev.objz.commandbridge.velocity.net.out.ExecuteCommandRequest;
import dev.objz.commandbridge.velocity.net.out.PingRequest;
import dev.objz.commandbridge.velocity.net.out.RegistrationRequest;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
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

    private ConfigManager configManager;
    private EndpointServer endpointServer;
    private RegistrationManager registrations;
    private InNode inNode;
    private OutNode<Object> outNode;
    private VelocityConfig cfg;
    private SessionHub sessions;
    private AuthHandler authHandler;
    private CBCommand command;
    private ScriptManager scriptManager;
    private CommandEntry commandEntry;
    private Object backendBootstrap;
    private ArgumentBridge argumentBridge;
    private PlatformFeatures platformFeatures;
    private boolean legacyDetected;
    private volatile String latestVersion;

    public static boolean isPapiEnabled = false;

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

        configManager = new ConfigManager(dataDir);
        boolean ok = configManager.load(VelocityConfig.class);
        cfg = configManager.current(VelocityConfig.class);
        if (!ok || cfg == null) {
            return;
        }
        Log.setDebug(cfg.debug());

        if (cfg.actAsClient()) {
            Log.warn("This instance is configured as a client-only. Not starting server");
            loadClientMode();
            return;
        }

        if (proxy.getPluginManager().getPlugin("papiproxybridge").isPresent()) {
            Log.success("Hooked into PapiProxyBridge. PlaceholderAPI is now enabled");
            isPapiEnabled = true;
        } else {
            Log.warn("PapiProxyBridge not found. PlaceholderAPI will not be used");
        }

        boolean hasPacketEvents = proxy.getPluginManager().getPlugin("packetevents").isPresent();
        var featuresBuilder = PlatformFeatureSet.builder();
        if (hasPacketEvents) {
            featuresBuilder.add(Location.VELOCITY, PlatformFeatureKeys.PACKET_EVENTS);
        } else {
            Log.warn("PacketEvents not found. Some placeholder may not be available on velocity");
        }
        platformFeatures = featuresBuilder.build();

        argumentBridge = new ArgumentBridge(platformFeatures);
        if (hasPacketEvents) {
            new PacketEventsArgumentBridge(argumentBridge.registry()).install();
        }

        sessions = new SessionHub();
        inNode = new InNode();
        outNode = new OutNode<>();
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

        commandEntry = new CommandEntry(proxy, pluginInstance, scriptManager, sessions, outNode,
                cfg.serverId(), dataDir);

        registrations.setCommandEntry(commandEntry);

        registrations.load(scriptManager.enabled());

        installRoutes();

        authHandler.onAuthenticated(registrations::onClientAuthenticated);

        command = new CBCommand(
                configManager,
                scriptManager,
                registrations,
                sessions,
                outNode,
                cfg);
        command.register();

        Log.debug("Config loaded:");
        Log.debug("  Endpoint Type: {}", cfg.endpointType());
        if (cfg.endpointType() == EndpointType.WEBSOCKET) {
            Log.debug("  WS Host: {}", cfg.endpoints().webSocket().bindHost());
            Log.debug("  WS Port: {}", cfg.endpoints().webSocket().bindPort());
        } else {
            Log.debug("  Redis Host: {}", cfg.endpoints().redis().host());
            Log.debug("  Redis Port: {}", cfg.endpoints().redis().port());
        }
        Log.debug("  Server ID: {}", cfg.serverId());

        checkForUpdate();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent e) {
        Log.info("Stopping CommandBridge");

        if (backendBootstrap != null) {
            try {
                backendBootstrap.getClass().getMethod("disable").invoke(backendBootstrap);
            } catch (Exception ex) {
                Log.error("Failed to disable backend mode: {}", ex.getMessage());
            }
        }

        if (endpointServer != null) {
            endpointServer.stop();
        }
    }

    private void installRoutes() {
        var secret = new SecretLoader(dataDir).loadOrCreate();
        var auth = new AuthService(secret);
        authHandler = new AuthHandler(auth, sessions, endpointServer);
        authHandler.register(inNode);

        inNode.register(MessageType.INVOKED_COMMAND, new InvokedCommandHandler(sessions, commandEntry));
        inNode.register(MessageType.EXECUTE_COMMAND_RESULT, new ExecuteCommandHandler(proxy));

        outNode.setEndpointSendFactory((endpoint, env) -> endpointServer.send(endpoint, env));
        outNode.register(MessageType.REGISTER_COMMANDS, new RegistrationRequest());
        outNode.register(MessageType.PING, new PingRequest());
        outNode.register(MessageType.EXECUTE_COMMAND, new ExecuteCommandRequest());
    }

    private void loadClientMode() {
        try {
            String bootstrapClass = "dev.objz.commandbridge.backends.platform.bootstrap.VelocityMain";
            Class<?> clazz = Class.forName(bootstrapClass);

            var ctor = clazz.getConstructor(ProxyServer.class, Logger.class, Path.class, Object.class);
            this.backendBootstrap = ctor.newInstance(proxy, velocityLogger, dataDir, pluginInstance);

            clazz.getMethod("load").invoke(backendBootstrap);
            clazz.getMethod("enable").invoke(backendBootstrap);

            Log.success(true, "CommandBridge running in Client Mode (Backend)");

        } catch (ClassNotFoundException ex) {
            Log.error("Could not find backend bootstrap class. " +
                    "Ensure the 'backends' module is included in your build");
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
