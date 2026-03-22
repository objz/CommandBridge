package dev.objz.commandbridge.velocity.net.in;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.security.AuthService;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.InNode;
import dev.objz.commandbridge.net.payloads.util.AuthRequestPayload;
import dev.objz.commandbridge.net.payloads.util.AuthResponsePayload;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.velocity.net.EndpointServer;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class AuthHandler extends InboundHandler {

    private final AuthService auth;
    private final SessionHub sessions;
    private final EndpointServer endpointServer;

    private volatile Consumer<ClientSession> onAuthed;

    public AuthHandler(AuthService auth, SessionHub sessions, EndpointServer endpointServer) {
        this.auth = Objects.requireNonNull(auth);
        this.sessions = Objects.requireNonNull(sessions);
        this.endpointServer = Objects.requireNonNull(endpointServer);
    }

    public void register(InNode router) {
        router.register(MessageType.AUTH_REQUEST, this);
    }

    public void onAuthenticated(Consumer<ClientSession> listener) {
        this.onAuthed = listener;
    }

    @Override
    public void accept(Endpoint endpoint, Envelope env) {
        if (env.type() != MessageType.AUTH_REQUEST || env.payload() == null) {
            endpointServer.close(endpoint);
            return;
        }

        AuthRequestPayload ap;
        try {
            ap = Envelope.MAPPER.treeToValue(env.payload(), AuthRequestPayload.class);
        } catch (Exception e) {
            Log.error(e, "Failed to handle AUTH_REQUEST from {}", env.from());
            reply(endpoint, env, MessageType.AUTH_FAIL, null)
                    .dispatch()
                    .exceptionally(ex -> {
                        Log.warn("Failed to send auth response: {}", ex.toString());
                        return null;
                    });
            endpointServer.close(endpoint);
            return;
        }

        if (ap == null || env.from() == null || ap.clientNonce() == null || ap.hmac() == null) {
            reply(endpoint, env, MessageType.AUTH_FAIL, null)
                    .dispatch()
                    .exceptionally(ex -> {
                        Log.warn("Failed to send auth response: {}", ex.toString());
                        return null;
                    });
            endpointServer.close(endpoint);
            Log.error("Authentication failed (malformed payload) from '{}'", endpoint.describe());
            return;
        }

        if (!auth.verify(env.from(), ap.clientNonce(), ap.hmac())) { // HMAC(clientId:clientNonce)
            reply(endpoint, env, MessageType.AUTH_FAIL, null)
                    .dispatch()
                    .exceptionally(ex -> {
                        Log.warn("Failed to send auth response: {}", ex.toString());
                        return null;
                    });
            endpointServer.close(endpoint);
            Log.error("Authentication failed for '{}' from '{}'", env.from(), endpoint.describe());
            return;
        }

        final String serverNonce = UUID.randomUUID().toString().replace("-", "");
        final String serverMac = auth.signServerProof(env.from(), ap.clientNonce(), serverNonce);

        sessions.get(env.from()).ifPresent(existing -> {
            if (existing.endpoint() != endpoint) {
                endpointServer.close(existing.endpoint());
            }
        });

        ClientSession s = sessions.add(env.from(), endpoint);
        s.location(ap.location() != null ? ap.location() : Location.BACKEND);
        s.status(AuthStatus.AUTH_OK);

        reply(endpoint, env, MessageType.AUTH_OK, new AuthResponsePayload(serverNonce, serverMac))
                .dispatch()
                .exceptionally(ex -> {
                    Log.warn("Failed to send auth response: {}", ex.toString());
                    return null;
                });
        Log.success(true, "Authentication succeeded for '{}' from '{}'", env.from(), endpoint.describe());

        var cb = onAuthed;
        if (cb != null) {
            try {
                cb.accept(s);
            } catch (Exception e) {
                Log.error("Authentication listener failed: {}", e.toString());
            }
        }
    }
}
