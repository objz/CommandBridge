package dev.objz.commandbridge.velocity.net.out.ctx;

import dev.objz.commandbridge.velocity.net.session.ClientSession;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record ResolveUuidRequestContext(
        ClientSession session,
        String name,
        Duration timeout,
        CompletableFuture<UUID> resultFuture) {

    public ResolveUuidRequestContext {
        Objects.requireNonNull(session);
        Objects.requireNonNull(name);
        Objects.requireNonNull(timeout);
        Objects.requireNonNull(resultFuture);
    }
}
