package dev.objz.commandbridge.velocity.net.out.ctx;

import dev.objz.commandbridge.velocity.net.session.ClientSession;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ResolveUuidRequestContext {
    public final ClientSession session;
    public final String name;
    public final Duration timeout;
    public final CompletableFuture<UUID> resultFuture;

    public ResolveUuidRequestContext(ClientSession session, String name, Duration timeout,
            CompletableFuture<UUID> resultFuture) {
        this.session = Objects.requireNonNull(session);
        this.name = Objects.requireNonNull(name);
        this.timeout = Objects.requireNonNull(timeout);
        this.resultFuture = Objects.requireNonNull(resultFuture);
    }
}
