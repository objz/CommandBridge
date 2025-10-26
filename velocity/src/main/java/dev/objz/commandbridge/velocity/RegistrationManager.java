package dev.objz.commandbridge.velocity;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.velocitypowered.api.proxy.ProxyServer;

import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.OutboundRouter;
import dev.objz.commandbridge.net.payloads.cmd.CommandStub;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.cmd.ArgumentMapper;
import dev.objz.commandbridge.velocity.cmd.CommandRegistry;
import dev.objz.commandbridge.velocity.net.out.RegistrationRequest;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;

public final class RegistrationManager {

	private final SessionHub sessions;
	private final CommandRegistry registry;
	private final OutboundRouter outRouter;
	private final Duration registerTimeout;

	private final Map<String, Set<Script>> backendByClient = new ConcurrentHashMap<>();
	private final Set<Script> velocityScripts = ConcurrentHashMap.newKeySet();

	public RegistrationManager(ProxyServer proxy,
			SessionHub sessions,
			VelocityConfig config,
			OutboundRouter outRouter) {
		this.sessions = Objects.requireNonNull(sessions);
		this.registry = new CommandRegistry(new ArgumentMapper(proxy));
		this.outRouter = Objects.requireNonNull(outRouter);
		this.registerTimeout = Duration.ofSeconds(
				Objects.requireNonNull(config).timeouts().registerTimeout());
	}

	public void load(List<Script> scripts) {
		clearState();

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

			var locations = s.register().stream()
					.filter(Objects::nonNull)
					.map(IdMapping::location)
					.filter(Objects::nonNull)
					.collect(toUnmodifiableSet());

			if (locations.contains(Location.VELOCITY)) {
				try {
					var stub = export(s);
					registry.register(stub);
					velocityScripts.add(s);
					velocityCollector.ok();
				} catch (Throwable e) {
					Log.error(e, "Velocity registration failed for '{}'", s.name());
					velocityCollector.fail("Velocity '" + s.name() + "': " + e.getMessage());
				}
			}

			if (locations.contains(Location.BACKEND)) {
				s.register().stream()
						.filter(m -> m != null && m.location() == Location.BACKEND)
						.map(IdMapping::id)
						.filter(Objects::nonNull)
						.forEach(backendId -> backendStaging
								.computeIfAbsent(backendId, k -> new ArrayList<>())
								.add(s));
			}
		}

		backendStaging.forEach((backendId, list) -> backendByClient.put(backendId, new HashSet<>(list)));

		if (velocityCollector.ok > 0) {
			Log.success(true, "Registered '{}' Velocity command(s)", velocityCollector.ok);
		}
		if (!velocityCollector.errors.isEmpty()) {
			Log.error("Failed to register '{}' Velocity command(s)", velocityCollector.errors.size());
			velocityCollector.errors.forEach(Log::error);
		}

		if (!backendByClient.isEmpty()) {
			int total = backendByClient.values().stream().mapToInt(Set::size).sum();
			Log.success(true, "Prepared '{}' backend command(s) for '{}' client(s)", total,
					backendByClient.size());
		}
	}

	public void onClientAuthenticated(ClientSession session) {
		var clientId = session.id();
		var scripts = backendByClient.get(clientId);
		if (scripts == null || scripts.isEmpty()) {
			Log.debug("No backend commands for '{}'", clientId);
			return;
		}

		outRouter.send(
				MessageType.REGISTER_COMMANDS,
				new RegistrationRequest.Args(session, scripts, registerTimeout));
	}

	// TODO: reload, but also make sure to reload all scripts and commands on
	// velocity
	public void reload() {
		for (var s : sessions) {
			if (s.status() == AuthStatus.AUTH_FAIL) {
				var set = backendByClient.get(s.id());
				if (set != null && !set.isEmpty()) {
					outRouter.send(
							MessageType.REGISTER_COMMANDS,
							new RegistrationRequest.Args(s, set, registerTimeout));
				}
			}
		}
	}

	public void shutdown() {
		try {
			registry.unregisterAll();
		} catch (Exception e) {
			Log.error(e, "Unregistering Velocity commands failed during shutdown");
		}
		clearState();
	}

	private void clearState() {
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
		List<ArgMapping> usedArgs = script.usedArguments();

		return new CommandStub(name, aliases, description, usedArgs);
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
