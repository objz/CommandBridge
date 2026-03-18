package dev.objz.commandbridge.velocity.net.out.ctx;

import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.velocity.net.session.ClientSession;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public record RegistrationRequestContext(
        ClientSession session,
        Set<Script> scripts,
        Duration timeout,
        Consumer<Boolean> resultCallback) {

    public RegistrationRequestContext(ClientSession session, Set<Script> scripts, Duration timeout) {
        this(session, scripts, timeout, null);
    }

    public RegistrationRequestContext {
        Objects.requireNonNull(session);
        Objects.requireNonNull(scripts);
        Objects.requireNonNull(timeout);
    }
}
