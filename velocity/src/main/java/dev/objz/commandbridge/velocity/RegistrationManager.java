package dev.objz.commandbridge.velocity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocitypowered.api.proxy.ProxyServer;

import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.logging.FeedbackLog;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.proto.Envelope;
import dev.objz.commandbridge.proto.MessageType;
import dev.objz.commandbridge.proto.cmd.CommandStub;
import dev.objz.commandbridge.proto.cmd.RegisterCommandsPayload;
import dev.objz.commandbridge.proto.feedback.Feedback;
import dev.objz.commandbridge.proto.feedback.FeedbackCollector;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.velocity.cmd.ArgumentMapper;
import dev.objz.commandbridge.velocity.cmd.CommandRegistry;
import dev.objz.commandbridge.velocity.ws.ClientSession;
import dev.objz.commandbridge.velocity.ws.SessionHub;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public final class RegistrationManager {
	private final ProxyServer proxy;
	private final SessionHub sessions;
	private final ObjectMapper mapper;
	private final CommandRegistry registry;
	private final int registerTimeoutSeconds;

	private final Map<String, Set<Script>> backendRegistrations = new ConcurrentHashMap<>();
	private final Set<Script> velocityRegistrations = ConcurrentHashMap.newKeySet();

	public RegistrationManager(ProxyServer proxy, SessionHub sessions, ObjectMapper mapper, VelocityConfig config) {
		this.proxy = proxy;
		this.sessions = sessions;
		this.mapper = mapper;
		this.registry = new CommandRegistry(new ArgumentMapper(proxy));
		this.registerTimeoutSeconds = config.timeouts().registerTimeout();

		sessions.onAuthed(this::handleClientAuthenticated);
	}

	public void loadScripts(List<Script> scripts) {
		if (scripts == null || scripts.isEmpty()) {
			Log.warn("No scripts to register");
			return;
		}

		backendRegistrations.clear();
		velocityRegistrations.clear();

		FeedbackCollector velocityCollector = new FeedbackCollector();
		Map<String, List<Script>> backendMap = new HashMap<>();

		for (Script script : scripts) {
			if (script == null || script.register() == null || script.register().isEmpty()) {
				Log.warn("Script '{}' has no registration targets",
						script != null ? script.name() : "<null>");
				continue;
			}

			Set<Location> locations = script.register().stream()
					.filter(Objects::nonNull)
					.map(IdMapping::location)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());

			if (locations.contains(Location.VELOCITY)) {
				velocityRegistrations.add(script);
				try {
					CommandStub stub = StubExporter.export(script);
					registry.register(stub);
					velocityCollector.success();
				} catch (Throwable e) {
					Log.error(e, "Failed to register Velocity command '{}'", script.name());
					velocityCollector.failure(
							"Velocity command '" + script.name() + "': " + e.getMessage());
				}
			}

			if (locations.contains(Location.BACKEND)) {
				script.register().stream()
						.filter(id -> id != null && id.location() == Location.BACKEND)
						.map(IdMapping::id)
						.filter(Objects::nonNull)
						.forEach(backendId -> {
							backendMap.computeIfAbsent(backendId, k -> new ArrayList<>())
									.add(script);
						});
			}
		}

		backendMap.forEach((backendId, scriptList) -> backendRegistrations.put(backendId,
				new HashSet<>(scriptList)));

		if (velocityCollector.succeeded() > 0) {
			Log.success(true, "Registered '{}' command(s)", velocityCollector.succeeded());
		}

		if (velocityCollector.failed() > 0) {
			Log.error("Failed to register '{}' Velocity command(s)", velocityCollector.failed());
			for (String error : velocityCollector.errors()) {
				Log.error(error);
			}
		}

		if (!backendRegistrations.isEmpty()) {
			int totalBackendCommands = backendRegistrations.values().stream()
					.mapToInt(Set::size)
					.sum();
			Log.success(true, "Prepared '{}' backend command(s) for '{}' client(s)",
					totalBackendCommands, backendRegistrations.size());
		}
	}

	public void reload(List<Script> scripts) {
		try {
			registry.unregisterAll();
		} catch (Exception e) {
			Log.error(e, "Failed to unregister Velocity commands during reload");
		}

		loadScripts(scripts);
	}

	private void handleClientAuthenticated(ClientSession session) {
		String clientId = session.clientId();
		Set<Script> scripts = backendRegistrations.get(clientId);

		if (scripts == null || scripts.isEmpty()) {
			Log.debug("No backend commands registered for client '{}'", clientId);
			return;
		}

		List<CommandStub> stubs = scripts.stream()
				.map(script -> {
					try {
						return StubExporter.export(script);
					} catch (Exception e) {
						Log.error(e, "Failed to export stub for script '{}'", script.name());
						return null;
					}
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		if (stubs.isEmpty()) {
			Log.warn("No valid command stubs to send to '{}'", clientId);
			return;
		}

		RegisterCommandsPayload payload = new RegisterCommandsPayload(false, stubs);

		try {
			Envelope env = Envelope.make(
					MessageType.REGISTER_COMMANDS,
					sessions.serverId(),
					clientId,
					mapper.valueToTree(payload));

			CompletableFuture<Envelope> feedbackFuture = new CompletableFuture<>();
			sessions.expectFeedback(String.valueOf(env.id()), clientId, feedbackFuture);
			sessions.send(session.ch(), env);

			// Await feedback with timeout
			feedbackFuture
					.orTimeout(registerTimeoutSeconds, TimeUnit.SECONDS)
					.thenAccept(feedbackEnv -> {
						try {
							Feedback feedback = mapper.treeToValue(feedbackEnv.payload(),
									Feedback.class);

							if (feedback.failed() > 0 || (feedback.errors() != null
									&& !feedback.errors().isEmpty())) {
								FeedbackLog.details(feedback, clientId);
							}
						} catch (Exception e) {
							Log.error(e, "Failed to process registration feedback from '{}'",
									clientId);
						}
					})
					.exceptionally(throwable -> {
						if (throwable instanceof TimeoutException ||
								throwable.getCause() instanceof TimeoutException) {
							Log.error("Timeout waiting for registration feedback from '{}' after {} seconds",
									clientId, registerTimeoutSeconds);
						} else {
							Log.error(throwable,
									"Failed to receive registration feedback from '{}'",
									clientId);
						}
						return null;
					});

		} catch (Exception e) {
			Log.error(e, "Failed to send REGISTER_COMMANDS to '{}'", clientId);
		}
	}

	public void shutdown() {
		try {
			registry.unregisterAll();
		} catch (Exception e) {
			Log.error(e, "Failed to unregister Velocity commands during shutdown");
		}

		backendRegistrations.clear();
		velocityRegistrations.clear();
	}
}
