package dev.objz.commandbridge.velocity.net.out;

import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.velocity.net.session.ClientSession;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * Context for registration requests.
 */
public final class RegistrationRequestContext {
	public final ClientSession session;
	public final Set<Script> scripts;
	public final Duration timeout;

	public RegistrationRequestContext(ClientSession session, Set<Script> scripts, Duration timeout) {
		this.session = Objects.requireNonNull(session);
		this.scripts = Objects.requireNonNull(scripts);
		this.timeout = Objects.requireNonNull(timeout);
	}
}
