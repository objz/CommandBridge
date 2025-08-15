package dev.objz.commandbridge.velocity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import dev.objz.commandbridge.main.config.ConfigManager;
import dev.objz.commandbridge.main.config.model.VelocityConfig;
import dev.objz.commandbridge.main.core.CommandRouter;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.security.AuthService;
import dev.objz.commandbridge.main.security.SecretLoader;
import dev.objz.commandbridge.main.security.TLS;
import dev.objz.commandbridge.main.ws.SessionHub;
import dev.objz.commandbridge.main.ws.WsServer;

import java.nio.file.Path;

import javax.net.ssl.SSLContext;

import org.slf4j.Logger;

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
		VelocityConfig config = configManager.load(VelocityConfig.class);
		Log.setDebug(config.debug());

		var secret = new SecretLoader(dataDir).loadOrCreate();
		var auth = new AuthService(secret);
		var mapper = new ObjectMapper();

		var sessions = new SessionHub(config, mapper);
		var router = new CommandRouter(mapper, sessions, auth, config.serverId());
		boolean is_tls = config.security().tls();
		SSLContext ssl = null;
		if (is_tls) {
			ssl = TLS.ensure(dataDir, "localhost");
		}

		var ws = is_tls
				? new WsServer(config.bindHost(), config.bindPort(), router, sessions, true, ssl)
				: new WsServer(config.bindHost(), config.bindPort(), router, sessions);

		ws.start();

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
