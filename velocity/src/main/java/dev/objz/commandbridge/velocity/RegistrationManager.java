package dev.objz.commandbridge.velocity;

import static java.util.stream.Collectors.toUnmodifiableSet;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.payloads.cmd.CommandStub;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.cmd.ArgumentMapper;
import dev.objz.commandbridge.velocity.cmd.CommandRegistry;
import dev.objz.commandbridge.velocity.dispatch.CommandEntry;
import dev.objz.commandbridge.velocity.net.out.ctx.RegistrationRequestContext;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RegistrationManager {

	private final SessionHub sessions;
	private final ProxyServer proxy;
	private final OutNode<Object> outNode;
	private final Duration registerTimeout;

	private final Map<String, Set<Script>> backendByClient = new ConcurrentHashMap<>();
	private final Set<Script> velocityScripts = ConcurrentHashMap.newKeySet();

	private volatile CommandRegistry registry;
	private volatile CommandEntry commandEntry;

	public RegistrationManager(ProxyServer proxy, SessionHub sessions, VelocityConfig config,
			OutNode<Object> outNode) {
		this.proxy = Objects.requireNonNull(proxy);
		this.sessions = Objects.requireNonNull(sessions);
		this.outNode = Objects.requireNonNull(outNode);
		this.registerTimeout = Duration.ofSeconds(Objects.requireNonNull(config).timeouts().registerTimeout());
	}

	public void setCommandEntry(CommandEntry commandEntry) {
		this.commandEntry = commandEntry;
		this.registry = new CommandRegistry(
				new ArgumentMapper(proxy),
				(cmdName, source, args, stub) -> {
					if (this.commandEntry != null) {
						this.commandEntry.executeFromVelocity(cmdName, source, args, stub);
					} else {
						Log.warn("CommandEntry not set, cannot execute command '{}'", cmdName);
					}
				});
	}

	public void load(List<Script> scripts) {
		clearState();

		if (registry == null) {
			registry = new CommandRegistry(new ArgumentMapper(proxy));
		}

		try {
			registry.unregisterAll();
		} catch (Exception e) {
			Log.error(e, "Failed to unregister all Velocity commands before reload");
		}

		if (scripts == null || scripts.isEmpty()) {
			Log.warn("No scripts to register");
			return;
		}

		var velocityCollector = new Counter();
		Map<String, List<Script>> backendStaging = new HashMap<>();

		for (Script s : scripts) {
			if (s == null || s.register() == null || s.register().isEmpty()) {
				Log.warn("Script '{}' has no registration targets", s != null ? s.name() : "<null>");
				continue;
			}

			var locations = s.register().stream().filter(Objects::nonNull).map(IdMapping::location)
					.filter(Objects::nonNull).collect(toUnmodifiableSet());

			if (locations.contains(Location.VELOCITY)) {
				try {
					var stub = export(s);
					registry.register(stub);
					velocityScripts.add(s);
					velocityCollector.ok();
				} catch (Throwable e) {
					Log.error(e, "Velocity registration failed for '{}'", s.name());
					velocityCollector.fail("Velocity '" + s.name() + "':  " + e.getMessage());
				}
			}

			if (locations.contains(Location.BACKEND)) {
				s.register().stream().filter(m -> m != null && m.location() == Location.BACKEND)
						.map(IdMapping::id).filter(Objects::nonNull)
						.forEach(backendId -> backendStaging
								.computeIfAbsent(backendId, k -> new ArrayList<>())
								.add(s));
			}
		}

		backendStaging.forEach((backendId, list) -> backendByClient.put(backendId, new HashSet<>(list)));

		int ok = velocityCollector.ok;
		if (ok > 0) {
			Log.success(true,
					"Registered '{}' " + Log.plural(ok, "Velocity command", "Velocity commands"),
					ok);
		}
		if (!velocityCollector.errors.isEmpty()) {
			Log.error("Failed to register '{}' "
					+ Log.plural(velocityCollector.errors.size(), "Velocity command",
							"Velocity commands"),
					velocityCollector.errors.size());
		}

		if (!backendByClient.isEmpty()) {
			int total = backendByClient.values().stream().mapToInt(Set::size).sum();
			int clients = backendByClient.size();
			Log.success(true,
					"Prepared '{}' " + Log.plural(total, "backend command", "backend commands")
							+ " for '{}' " + Log.plural(clients, "client", "clients"),
					total, clients);
		}
	}

	public Set<Script> getScriptsForClient(String clientId) {
		return backendByClient.get(clientId);
	}

	public void onClientAuthenticated(ClientSession session) {
		var clientId = session.id();
		var scripts = backendByClient.get(clientId);
		if (scripts == null || scripts.isEmpty()) {
			Log.debug("No backend commands for '{}'", clientId);
			return;
		}

		outNode.send(MessageType.REGISTER_COMMANDS,
				new RegistrationRequestContext(session, scripts, registerTimeout));
	}

	public void reload() {
		for (var s : sessions) {
			if (s.status() == AuthStatus.AUTH_OK && s.ch() != null && s.ch().isOpen()) {
				var set = backendByClient.get(s.id());
				if (set != null && !set.isEmpty()) {
					outNode.send(MessageType.REGISTER_COMMANDS,
							new RegistrationRequestContext(s, set, registerTimeout));
				}
			}
		}
	}

	public void clearState() {
		backendByClient.clear();
		velocityScripts.clear();
	}

	private static CommandStub export(Script script) {
		if (script == null)
			throw new IllegalArgumentException("Script cannot be null");

		String name = script.name();
		if (name == null || name.isBlank())
			throw new IllegalArgumentException("Script name cannot be null or blank");

		List<String> aliases = script.aliases() != null ? script.aliases() : List.of();
		String description = script.description();
		List<ArgMapping> registeredArgs = script.registeredArguments();

		return new CommandStub(name, aliases, description, registeredArgs);
	}

	private static final class Counter {
		int ok = 0;
		final List<String> errors = new ArrayList<>();

		void ok() {
			ok++;
		}

		void fail(String msg) {
			errors.add(msg);
		}
	}
}
