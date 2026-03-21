package dev.objz.commandbridge.backends.net.routing;

import dev.objz.commandbridge.backends.net.connection.ClientStatus;
import dev.objz.commandbridge.api.platform.ConnectionState;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.backends.net.out.ctx.AuthRequestContext;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class AuthHandler {
    private final BackendsConfig cfg;
    private final OutNode outNode;
    private final AtomicReference<ConnectionState> stateRef;
    private volatile Runnable onAuthed;

    public AuthHandler(BackendsConfig cfg, OutNode outNode, AtomicReference<ConnectionState> stateRef) {
        this.cfg = cfg;
        this.outNode = outNode;
        this.stateRef = stateRef;
    }

    public void onAuthenticated(Runnable callback) {
        this.onAuthed = callback;
    }

    public boolean authenticate() {

        Consumer<ClientStatus> statusUpdater = status -> {
            ConnectionState newState = status.toConnectionState();
            stateRef.set(newState);

            if (newState == ConnectionState.AUTHENTICATED) {
                Log.debug("Authentication successful");
                var cb = onAuthed;
                if (cb != null) {
                    try {
                        cb.run();
                    } catch (Exception e) {
                        Log.error("Authentication callback failed: {}", e.getMessage());
                    }
                }
            } else if (newState == ConnectionState.AUTH_FAILED) {
                Log.error("Authentication failed");
            }
        };

        Duration timeout = Duration.ofSeconds(cfg.timeouts().authTimeout());
        AuthRequestContext context = new AuthRequestContext(timeout, statusUpdater);

        try {
            outNode.send(MessageType.AUTH_REQUEST, context);
            return true;
        } catch (Exception e) {
            Log.error(e, "Failed to send authentication request");
            stateRef.set(ConnectionState.AUTH_FAILED);
            return false;
        }
    }

    public boolean isAuthenticated() {
        return stateRef.get() == ConnectionState.AUTHENTICATED;
    }

    public ConnectionState getState() {
        return stateRef.get();
    }
}
