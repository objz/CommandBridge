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

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIHandler;
import dev.jorel.commandapi.CommandAPIPlatform;
import dev.jorel.commandapi.CommandAPIVelocityConfig;
import dev.jorel.commandapi.InternalConfig;
import dev.jorel.commandapi.LoadContext;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.security.AuthService;
import dev.objz.commandbridge.security.SecretLoader;
import dev.objz.commandbridge.security.TlsResolver;
import dev.objz.commandbridge.velocity.ws.MessageRouter;
import dev.objz.commandbridge.velocity.ws.SessionHub;
import dev.objz.commandbridge.velocity.ws.WsServer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Path;

@Plugin(id = "commandbridge", name = "CommandBridge", version = "3.0.0", url = "https://cb.objz.dev", description = "I did it!", authors = {
		"objz" })
public final class Main {
	private final ProxyServer server;
	private final Path dataDir;
	private ConfigManager configManager;
	private static boolean commandAPILoaded = false;
	private WsServer ws;

	@Inject
	public Main(ProxyServer server, Logger velocityLogger, @DataDirectory Path dataDir) {
		this.server = server;
		this.dataDir = dataDir;
		Log.install(velocityLogger);

		// this is so ugly but I don't think there is a better way
		if (!commandAPILoaded) {
			try {
				CommandAPIVelocityConfig velocityConfig = new CommandAPIVelocityConfig(server, this)
						.silentLogs(false)
						.verboseOutput(false);

				InternalConfig internalConfig = new InternalConfig(velocityConfig);

				Field configField = CommandAPI.class.getDeclaredField("config");
				configField.setAccessible(true);
				configField.set(null, internalConfig);

				Class<?> internalVelocityConfigClass = Class
						.forName("dev.jorel.commandapi.InternalVelocityConfig");
				Object internalVelocityConfig = internalVelocityConfigClass
						.getDeclaredConstructor(CommandAPIVelocityConfig.class)
						.newInstance(velocityConfig);

				Class<?> commandAPIVelocityClass = Class
						.forName("dev.jorel.commandapi.CommandAPIVelocity");
				CommandAPIPlatform<?, ?, ?> velocityPlatform = (CommandAPIPlatform<?, ?, ?>) commandAPIVelocityClass
						.getDeclaredConstructor(internalVelocityConfigClass)
						.newInstance(internalVelocityConfig);

				LoadContext loadContext = new LoadContext(velocityPlatform);

				Constructor<?> handlerConstructor = CommandAPIHandler.class
						.getDeclaredConstructor(CommandAPIPlatform.class);
				handlerConstructor.setAccessible(true);
				handlerConstructor.newInstance(loadContext.platform());

				loadContext.context().run();

				CommandAPIHandler.getInstance().onLoad();

				Field loadedField = CommandAPI.class.getDeclaredField("loaded");
				loadedField.setAccessible(true);
				loadedField.set(null, true);

				StringBuilder currentStack = new StringBuilder();
				for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
					currentStack.append(e.toString()).append("\n");
				}
				Field loadedStackField = CommandAPI.class.getDeclaredField("loadedStack");
				loadedStackField.setAccessible(true);
				loadedStackField.set(null, currentStack.toString());

				commandAPILoaded = true;
				Log.info("CommandAPI loaded");

			} catch (Exception e) {
				throw new RuntimeException("CommandAPI initialization failed", e);
			}
		}
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

		CommandAPI.onEnable();

		var secret = new SecretLoader(dataDir).loadOrCreate();
		var auth = new AuthService(secret);
		var mapper = new ObjectMapper();

		var sessions = new SessionHub(config.serverId()); 
		boolean requireAuth = config.security().requireAuth();
		var router = new MessageRouter(mapper, sessions, auth, config.serverId(), requireAuth, config);

		var tls = TlsResolver.resolveServer(dataDir, config.security());
		this.ws = tls.enabled()
				? new WsServer(config.bindHost(), config.bindPort(), router, sessions, true,
						tls.context())
				: new WsServer(config.bindHost(), config.bindPort(), router, sessions);
		ws.start();

		var scriptManager = new ScriptManager(dataDir);
		scriptManager.loadAll();

		var registrationManager = new RegistrationManager(server, sessions, mapper, config);
		registrationManager.loadScripts(scriptManager.enabled());

		Log.debug("Config loaded:");
		Log.debug("  Host: {}", config.bindHost());
		Log.debug("  Port: {}", config.bindPort());
		Log.debug("  Server ID: {}", config.serverId());
	}

	@Subscribe
	public void onProxyShutdown(ProxyShutdownEvent event) {
		Log.info("Stopping CommandBridge...");
	}
}
