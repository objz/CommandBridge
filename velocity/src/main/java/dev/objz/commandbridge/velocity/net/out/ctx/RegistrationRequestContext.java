package dev.objz.commandbridge.velocity.net.out.ctx;

import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.velocity.net.session.ClientSession;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public final class RegistrationRequestContext {
	public final ClientSession session;
	public final Set<Script> scripts;
	public final Duration timeout;
	public final Consumer<Boolean> resultCallback;

	public RegistrationRequestContext(ClientSession session, Set<Script> scripts, Duration timeout) {
		this(session, scripts, timeout, null);
	}

	public RegistrationRequestContext(ClientSession session, Set<Script> scripts, Duration timeout,
			Consumer<Boolean> resultCallback) {
		this.session = Objects.requireNonNull(session);
		this.scripts = Objects.requireNonNull(scripts);
		this.timeout = Objects.requireNonNull(timeout);
		this.resultCallback = resultCallback;
	}
}
