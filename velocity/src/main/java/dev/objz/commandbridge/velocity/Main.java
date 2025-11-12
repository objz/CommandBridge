package dev.objz.commandbridge.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.InNode;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.security.AuthService;
import dev.objz.commandbridge.security.SecretLoader;
import dev.objz.commandbridge.security.TlsResolver;
import dev.objz.commandbridge.velocity.cli.CBCommand;
import dev.objz.commandbridge.velocity.net.WsServer;
import dev.objz.commandbridge.velocity.net.in.AuthHandler;
import dev.objz.commandbridge.velocity.net.out.PingRequest;
import dev.objz.commandbridge.velocity.net.out.RegistrationRequest;
import dev.objz.commandbridge.velocity.net.session.SessionHub;

import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "commandbridge", name = "CommandBridge", version = "3.0.0", url = "https://cb.objz.dev", description = "I did it!", authors = {
		"objz" }, dependencies = { @Dependency(id = "commandapi") })
public final class Main {

	private final ProxyServer proxy;
	private final Path dataDir;

	private ConfigManager configManager;
	private WsServer ws;
	private RegistrationManager registrations;
	private InNode inNode;
	private OutNode<Object> outNode;
	private VelocityConfig cfg;
	private SessionHub sessions;
	private AuthHandler authHandler;
	private CBCommand command;

	@Inject
	public Main(ProxyServer proxy, Logger velocityLogger, @DataDirectory Path dataDir) {
		this.proxy = proxy;
		this.dataDir = dataDir;
		Log.install(velocityLogger);
	}

	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent e) {
		Log.info("Initializing CommandBridge");

		configManager = new ConfigManager(dataDir);
		boolean ok = configManager.load(VelocityConfig.class);
		cfg = configManager.current(VelocityConfig.class);
		if (!ok || cfg == null) {
			return;
		}
		Log.setDebug(cfg.debug());

		sessions = new SessionHub();
		inNode = new InNode();
		outNode = new OutNode<>();
		outNode.setServerId(cfg.serverId());

		var tls = TlsResolver.resolveServer(dataDir, cfg.security());
		ws = tls.enabled()
				? new WsServer(cfg.bindHost(), cfg.bindPort(), sessions, inNode,
						true, tls.context())
				: new WsServer(cfg.bindHost(), cfg.bindPort(), sessions, inNode);
		ws.start();

		var scriptManager = new ScriptManager(dataDir);
		scriptManager.loadAll();

		registrations = new RegistrationManager(proxy, sessions, cfg, outNode);
		registrations.load(scriptManager.enabled());
		// install routes after initalizing but before registering any listeners
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
		Log.debug("  Host: {}", cfg.bindHost());
		Log.debug("  Port: {}", cfg.bindPort());
		Log.debug("  Server ID: {}", cfg.serverId());
	}

	@Subscribe
	public void onProxyShutdown(ProxyShutdownEvent e) {
		Log.info("Stopping CommandBridge");
		if (registrations != null) {
			registrations.clearState();
		}
		if (ws != null) {
			ws.stop();
		}
	}

	private void installRoutes() {
		var secret = new SecretLoader(dataDir).loadOrCreate();
		var auth = new AuthService(secret);
		authHandler = new AuthHandler(auth, sessions, ws);
		authHandler.register(inNode);

		outNode.setChannelSendOperationFactory((ch, env) -> ws.send(ch, env));
		outNode.register(MessageType.REGISTER_COMMANDS, new RegistrationRequest());
		outNode.register(MessageType.PING, new PingRequest());
	}
}
