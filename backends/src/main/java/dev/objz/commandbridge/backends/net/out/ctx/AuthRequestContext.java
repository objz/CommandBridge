package dev.objz.commandbridge.backends.net.out.ctx;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

import dev.objz.commandbridge.backends.net.ClientStatus;

public final class AuthRequestContext {
    public final Duration timeout;
    public final Consumer<ClientStatus> statusSink;

    public AuthRequestContext(Duration timeout, Consumer<ClientStatus> statusSink) {
        this.timeout = Objects.requireNonNull(timeout);
        this.statusSink = Objects.requireNonNull(statusSink);
    }
}
