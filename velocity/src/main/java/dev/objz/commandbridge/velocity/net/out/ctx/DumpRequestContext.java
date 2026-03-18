package dev.objz.commandbridge.velocity.net.out.ctx;

import dev.objz.commandbridge.velocity.net.session.ClientSession;

import java.time.Duration;
import java.util.Objects;

public record DumpRequestContext(
        ClientSession session,
        Duration timeout) {

    public DumpRequestContext {
        Objects.requireNonNull(session);
        Objects.requireNonNull(timeout);
    }
}
