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
    private final Object lifecycleLock;
    private long generation;
    private volatile Runnable onAuthed;
    private volatile Runnable onAuthFailed;

    public AuthHandler(BackendsConfig cfg, OutNode outNode, AtomicReference<ConnectionState> stateRef,
            Object lifecycleLock) {
        this.cfg = cfg;
        this.outNode = outNode;
        this.stateRef = stateRef;
        this.lifecycleLock = lifecycleLock;
    }

    public void onAuthenticated(Runnable callback) {
        this.onAuthed = callback;
    }

    public void onAuthFailed(Runnable callback) {
        this.onAuthFailed = callback;
    }

    public boolean authenticate() {
        synchronized (lifecycleLock) {
            return authenticateLocked();
        }
    }

    public void invalidate() {
        synchronized (lifecycleLock) {
            generation++;
        }
    }

    private boolean authenticateLocked() {
        long attempt = ++generation;

        Consumer<ClientStatus> statusUpdater = status -> {
            synchronized (lifecycleLock) {
                if (attempt != generation) {
                    return;
                }
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
            }
        };

        Duration timeout = Duration.ofSeconds(cfg.timeouts().authTimeout());
        AuthRequestContext context = new AuthRequestContext(timeout, statusUpdater, () -> {
            synchronized (lifecycleLock) {
                if (attempt == generation && onAuthFailed != null) {
                    onAuthFailed.run();
                }
            }
        });

        try {
            outNode.send(MessageType.AUTH_REQUEST, context);
            return true;
        } catch (Exception e) {
            Log.error(e, "Failed to send authentication request");
            if (attempt == generation) {
                stateRef.set(ConnectionState.AUTH_FAILED);
            }
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
