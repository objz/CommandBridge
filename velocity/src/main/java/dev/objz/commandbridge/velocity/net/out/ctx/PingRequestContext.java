package dev.objz.commandbridge.velocity.net.out.ctx;

import dev.objz.commandbridge.velocity.net.session.ClientSession;

import java.time.Duration;
import java.util.Objects;
import java.util.function.BiConsumer;

public record PingRequestContext(
        ClientSession session,
        Duration timeout,
        BiConsumer<Boolean, Long> resultCallback) {

    public PingRequestContext {
        Objects.requireNonNull(session);
        Objects.requireNonNull(timeout);
        Objects.requireNonNull(resultCallback);
    }
}
