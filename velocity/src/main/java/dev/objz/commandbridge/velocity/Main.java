package dev.objz.commandbridge.velocity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.security.AuthService;
import dev.objz.commandbridge.security.SecretLoader;
import dev.objz.commandbridge.security.TlsResolver;
import dev.objz.commandbridge.velocity.registry.OnAuthRegisterCommands;
import dev.objz.commandbridge.velocity.registry.ScriptManager;
import dev.objz.commandbridge.velocity.ws.MessageRouter;
import dev.objz.commandbridge.velocity.ws.SessionHub;
import dev.objz.commandbridge.velocity.ws.WsServer;

import java.nio.file.Path;

@Plugin(id = "commandbridge", name = "CommandBridge", version = "3.0.0", url = "https://cb.objz.dev", description = "I did it!", authors = {
		"objz" })
public final class Main {
	private final ProxyServer server;
	private final Path dataDir;
	private ConfigManager configManager;

	@Inject
	public Main(ProxyServer server, Logger velocityLogger, @DataDirectory Path dataDir) {
		this.server = server;
		this.dataDir = dataDir;
		Log.install(velocityLogger);
	}

	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent event) {
		Log.info("Initializing CommandBridge...");

		this.configManager = new ConfigManager(dataDir);
		boolean ok = configManager.load(VelocityConfig.class);
		VelocityConfig config = configManager.current(VelocityConfig.class);
		if (!ok || config == null) {
			Log.error("Failed to load velocity config; aborting enable.");
			return;
		}
		Log.setDebug(config.debug());

		var secret = new SecretLoader(dataDir).loadOrCreate();
		var auth = new AuthService(secret);
		var mapper = new ObjectMapper();

		var sessions = new SessionHub();
		boolean requireAuth = config.security().requireAuth();
		var router = new MessageRouter(mapper, sessions, auth, config.serverId(), requireAuth, config);

		var tls = TlsResolver.resolveServer(dataDir, config.security());
		var ws = tls.enabled()
				? new WsServer(config.bindHost(), config.bindPort(), router, sessions, true,
						tls.context())
				: new WsServer(config.bindHost(), config.bindPort(), router, sessions);
		ws.start();

		var scriptManager = new ScriptManager(dataDir);
		scriptManager.loadAll();

		OnAuthRegisterCommands.install(
				sessions,
				scriptManager,
				mapper,
				config.serverId(),
				config);

		Log.debug("Config loaded:");
		Log.debug("  Host: {}", config.bindHost());
		Log.debug("  Port: {}", config.bindPort());
		Log.debug("  Server ID: {}", config.serverId());
		Log.debug("  Heartbeat: {}s ping, {}s stale timeout",
				config.heartbeat().appPingSeconds(),
				config.heartbeat().staleAfterSeconds());
		Log.debug("  RequireAuth: {}", config.security().requireAuth());
	}

	@Subscribe
	public void onProxyShutdown(ProxyShutdownEvent event) {
		Log.info("Stopping CommandBridge...");
	}
}
