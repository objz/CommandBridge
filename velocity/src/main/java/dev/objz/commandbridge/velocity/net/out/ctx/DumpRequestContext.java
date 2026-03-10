package dev.objz.commandbridge.velocity.net.out.ctx;

import dev.objz.commandbridge.velocity.net.session.ClientSession;

import java.time.Duration;
import java.util.Objects;

public final class DumpRequestContext {
    public final ClientSession session;
    public final Duration timeout;

    public DumpRequestContext(ClientSession session, Duration timeout) {
        this.session = Objects.requireNonNull(session);
        this.timeout = Objects.requireNonNull(timeout);
    }
}
