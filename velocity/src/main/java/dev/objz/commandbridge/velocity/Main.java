package dev.objz.commandbridge.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import dev.objz.commandbridge.main.config.ConfigManager;
import dev.objz.commandbridge.main.config.model.VelocityConfig;
import dev.objz.commandbridge.main.logging.Log;

import java.nio.file.Path;

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
		Log.setDebug(true);
		Log.info("Initializing CommandBridge...");
		this.configManager = new ConfigManager(dataDir);
		VelocityConfig config = configManager.load();
		Log.setDebug(config.debug());

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
