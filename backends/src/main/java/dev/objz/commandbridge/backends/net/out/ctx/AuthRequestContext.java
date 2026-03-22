package dev.objz.commandbridge.backends.net.out.ctx;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

import dev.objz.commandbridge.backends.net.connection.ClientStatus;

public record AuthRequestContext(
        Duration timeout,
        Consumer<ClientStatus> statusSink,
        Runnable onAuthFailed) {

    public AuthRequestContext(Duration timeout, Consumer<ClientStatus> statusSink) {
        this(timeout, statusSink, null);
    }

    public AuthRequestContext {
        Objects.requireNonNull(timeout);
        Objects.requireNonNull(statusSink);
    }
}
