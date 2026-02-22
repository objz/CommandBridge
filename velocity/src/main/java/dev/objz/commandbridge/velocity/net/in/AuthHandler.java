package dev.objz.commandbridge.velocity.net.in;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.security.AuthService;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.InNode;
import dev.objz.commandbridge.net.payloads.util.AuthRequestPayload;
import dev.objz.commandbridge.net.payloads.util.AuthResponsePayload;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.velocity.net.WsServer;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class AuthHandler extends InboundHandler {

    private final AuthService auth;
    private final SessionHub sessions;
    private final WsServer ws;

    private volatile Consumer<ClientSession> onAuthed;

    public AuthHandler(AuthService auth, SessionHub sessions, WsServer ws) {
        this.auth = Objects.requireNonNull(auth);
        this.sessions = Objects.requireNonNull(sessions);
        this.ws = Objects.requireNonNull(ws);
    }

    public void register(InNode router) {
        router.register(MessageType.AUTH_REQUEST, this);
    }

    public void onAuthenticated(Consumer<ClientSession> listener) {
        this.onAuthed = listener;
    }

    @Override
    public void accept(WebSocketChannel ch, Envelope env) {
        if (env.type() != MessageType.AUTH_REQUEST || env.payload() == null) {
            ws.close(ch);
            return;
        }

        AuthRequestPayload ap;
        try {
            ap = Envelope.MAPPER.treeToValue(env.payload(), AuthRequestPayload.class);
        } catch (Exception e) {
            Log.error(e, "Failed to handle AUTH_REQUEST from {}", env.from());
            reply(ch, env, MessageType.AUTH_FAIL, null)
                    .dispatch()
                    .exceptionally(ex -> {
                        Log.warn("Failed to send auth response: {}", ex.toString());
                        return null;
                    });
            ws.close(ch);
            return;
        }

        if (ap == null || env.from() == null || ap.clientNonce() == null || ap.hmac() == null) {
            reply(ch, env, MessageType.AUTH_FAIL, null)
                    .dispatch()
                    .exceptionally(ex -> {
                        Log.warn("Failed to send auth response: {}", ex.toString());
                        return null;
                    });
            ws.close(ch);
            Log.error("Authentication failed (malformed payload) from '{}'", ch.getSourceAddress());
            return;
        }

        if (!auth.verify(env.from(), ap.clientNonce(), ap.hmac())) { // HMAC(clientId:clientNonce)
            reply(ch, env, MessageType.AUTH_FAIL, null)
                    .dispatch()
                    .exceptionally(ex -> {
                        Log.warn("Failed to send auth response: {}", ex.toString());
                        return null;
                    });
            ws.close(ch);
            Log.error("Authentication failed for '{}' from '{}'", env.from(), ch.getSourceAddress());
            return;
        }

        final String serverNonce = UUID.randomUUID().toString().replace("-", "");
        final String serverMac = auth.signServerProof(env.from(), ap.clientNonce(), serverNonce);

        ClientSession s = sessions.add(ch, env.from());
        s.status(AuthStatus.AUTH_FAIL);

        s.location(ap.location() != null ? ap.location() : Location.BACKEND);

        reply(ch, env, MessageType.AUTH_OK, new AuthResponsePayload(serverNonce, serverMac))
                .dispatch()
                .exceptionally(ex -> {
                    Log.warn("Failed to send auth response: {}", ex.toString());
                    return null;
                });
        s.status(AuthStatus.AUTH_OK);
        Log.success(true, "Authentication succeeded for '{}' from '{}'", env.from(), ch.getSourceAddress());

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
