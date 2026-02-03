package dev.objz.commandbridge.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.config.model.VelocityConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.payloads.cmd.CommandStub;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;
import dev.objz.commandbridge.velocity.cmd.ArgumentMapper;
import dev.objz.commandbridge.velocity.cmd.CommandRegistry;
import dev.objz.commandbridge.velocity.cmd.bridge.framework.CustomArgumentRegistry;
import dev.objz.commandbridge.velocity.dispatch.CommandEntry;
import dev.objz.commandbridge.velocity.net.out.ctx.RegistrationRequestContext;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class RegistrationManager {

	private record TargetKey(String id, Location location) {
	}

	private final ProxyServer proxy;
	private final SessionHub sessions;
	private final OutNode<Object> outNode;
	private final String localServerId;
	private final Duration registerTimeout;
	private final CustomArgumentRegistry argumentRegistry;

	private final Map<TargetKey, Set<Script>> remoteScripts = new ConcurrentHashMap<>();

	private final Set<Script> localScripts = ConcurrentHashMap.newKeySet();

	private volatile CommandRegistry registry;
	private volatile CommandEntry commandEntry;

	public RegistrationManager(ProxyServer proxy, SessionHub sessions, VelocityConfig config,
			OutNode<Object> outNode, CustomArgumentRegistry argumentRegistry) {
		this.proxy = Objects.requireNonNull(proxy);
		this.sessions = Objects.requireNonNull(sessions);
		this.outNode = Objects.requireNonNull(outNode);
		this.localServerId = config.serverId();
		this.registerTimeout = Duration.ofSeconds(config.timeouts().registerTimeout());
		this.argumentRegistry = Objects.requireNonNull(argumentRegistry);
	}

	public void setCommandEntry(CommandEntry commandEntry) {
		this.commandEntry = commandEntry;
		this.registry = new CommandRegistry(
				new ArgumentMapper(proxy, argumentRegistry),
				argumentRegistry,
				(cmdName, source, args, stub) -> {
					if (this.commandEntry != null) {
						this.commandEntry.executeFromVelocity(cmdName, source, args, stub);
					} else {
						Log.warn("CommandEntry not set, dropping execution for '{}'", cmdName);
					}
				});
	}

	public void load(List<Script> scripts) {
		reset();

		if (registry == null) {
			registry = new CommandRegistry(new ArgumentMapper(proxy, argumentRegistry),
					argumentRegistry);
		}

		if (scripts == null || scripts.isEmpty()) {
			return;
		}

		int localCount = 0;

		for (Script script : scripts) {
			if (!isValid(script))
				continue;

			for (IdMapping target : script.register()) {
				if (isLocalTarget(target)) {
					if (registerLocal(script)) {
						localCount++;
					}
				} else {
					bufferRemote(script, target);
				}
			}
		}

		Log.success(true, "Registered '{}' local " + Log.plural(localCount, "command", "commands"), localCount);
		if (!remoteScripts.isEmpty()) {
			Log.success(true, "Buffered remote commands for '{}' " +
					Log.plural(remoteScripts.size(), "client", "clients"), remoteScripts.size());
		}
	}

	public void onClientAuthenticated(ClientSession session) {
		TargetKey key = new TargetKey(session.id(), session.location());
		Set<Script> scripts = remoteScripts.get(key);

		if (scripts == null || scripts.isEmpty()) {
			Log.debug("Client '{}' ({}) connected, but no scripts are targeted for it",
					session.id(), session.location());
			return;
		}

		Log.info("Syncing '{}' {} to client '{}' ({})", scripts.size(), Log.plural(scripts.size(), "client", "clients"), session.id(), session.location());
		outNode.send(MessageType.REGISTER_COMMANDS,
				new RegistrationRequestContext(session, scripts, registerTimeout));
	}

	public void reload() {
		for (ClientSession session : sessions) {
			if (session.id() != null) {
				onClientAuthenticated(session);
			}
		}
	}

	public Set<Script> getScriptsForSession(ClientSession session) {
		if (session == null)
			return Set.of();
		return remoteScripts.getOrDefault(new TargetKey(session.id(), session.location()), Set.of());
	}

	public void reset() {
		remoteScripts.clear();
		localScripts.clear();
		try {
			if (registry != null)
				registry.unregisterAll();
		} catch (Exception e) {
			Log.error("Failed to unregister commands during reset: {}", e.getMessage());
		}
	}

	private boolean registerLocal(Script script) {
		if (localScripts.contains(script))
			return false;

		try {
			CommandStub stub = export(script);
			registry.register(stub);
			localScripts.add(script);
			return true;
		} catch (Exception e) {
			Log.error("Failed to register local command '{}': {}", script.name(), e.getMessage());
			return false;
		}
	}

	private void bufferRemote(Script script, IdMapping target) {
		TargetKey key = new TargetKey(target.id(), target.location());
		remoteScripts.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(script);
	}

	private boolean isLocalTarget(IdMapping target) {
		return target.location() == Location.VELOCITY && target.id().equals(localServerId);
	}

	private boolean isValid(Script script) {
		return script != null
				&& script.name() != null
				&& !script.name().isBlank()
				&& script.register() != null
				&& !script.register().isEmpty();
	}

	private static CommandStub export(Script script) {
		return new CommandStub(
				script.name(),
				script.aliases() != null ? script.aliases() : List.of(),
				script.description(),
				script.registeredArguments());
	}
}
