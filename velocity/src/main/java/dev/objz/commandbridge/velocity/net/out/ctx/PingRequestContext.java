package dev.objz.commandbridge.velocity.net.out.ctx;

import dev.objz.commandbridge.velocity.net.session.ClientSession;

import java.time.Duration;
import java.util.Objects;
import java.util.function.BiConsumer;

public final class PingRequestContext {
    public final ClientSession session;
    public final Duration timeout;
    public final BiConsumer<Boolean, Long> resultCallback;

    public PingRequestContext(ClientSession session, Duration timeout, BiConsumer<Boolean, Long> resultCallback) {
        this.session = Objects.requireNonNull(session);
        this.timeout = Objects.requireNonNull(timeout);
        this.resultCallback = Objects.requireNonNull(resultCallback);
    }
}
