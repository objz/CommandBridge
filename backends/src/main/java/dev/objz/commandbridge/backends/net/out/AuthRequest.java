package dev.objz.commandbridge.backends.net.out;

import dev.objz.commandbridge.backends.net.connection.ClientStatus;
import dev.objz.commandbridge.backends.net.out.ctx.AuthRequestContext;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.OutboundHandler;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.payloads.util.AuthRequestPayload;
import dev.objz.commandbridge.net.payloads.util.AuthResponsePayload;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.security.AuthService;

import java.util.Objects;
import java.util.UUID;

public final class AuthRequest extends OutboundHandler<AuthRequestContext> {

    private final AuthService auth;
    private final Location location;

    public AuthRequest(AuthService auth, Location location) {
        this.auth = Objects.requireNonNull(auth);
        this.location = Objects.requireNonNull(location);
    }

    @Override
    public SendOperation accept(AuthRequestContext ctx) {
        final String clientNonce = UUID.randomUUID().toString();
        final String mac = auth.sign(clientId, clientNonce);

        var payload = Envelope.MAPPER.valueToTree(new AuthRequestPayload(location, clientNonce, mac));
        Envelope env = Envelope.make(MessageType.AUTH_REQUEST, clientId, "proxy-auth", payload);

        SendOperation op = send(env)
                .match(reply -> reply.id().equals(env.id())
                        && (reply.type() == MessageType.AUTH_OK
                                || reply.type() == MessageType.AUTH_FAIL))
                .timeout(ctx.timeout);

        op.await().thenApply(reply -> {
            if (reply.type() == MessageType.AUTH_FAIL) {
                ctx.statusSink.accept(ClientStatus.AUTH_FAILED);
                Log.error("Authentication rejected by server");
                return reply;
            }

            AuthResponsePayload sp;
            try {
                sp = Envelope.MAPPER.treeToValue(reply.payload(),
                        AuthResponsePayload.class);
            } catch (Exception e) {
                ctx.statusSink.accept(ClientStatus.AUTH_FAILED);
                Log.error(e, "Authentication response malformed");
                return reply;
            }

            final String serverNonce = sp.serverNonce();
            final String serverMac = sp.hmac();
            final boolean ok = auth.verifyServerProof(clientId, clientNonce, serverNonce,
                    serverMac);

            if (!ok) {
                ctx.statusSink.accept(ClientStatus.AUTH_FAILED);
                Log.error("Authentication failed: invalid server proof");
            } else {
                ctx.statusSink.accept(ClientStatus.AUTH_OK);
                Log.success("Authenticated successfully");
            }
            return reply;
        })
                .exceptionally(ex -> {
                    ctx.statusSink.accept(ClientStatus.AUTH_FAILED);
                    var cause = (ex.getCause() != null) ? ex.getCause() : ex;
                    if (cause instanceof java.util.concurrent.TimeoutException) {
                        Log.error("Authentication timeout");
                    } else {
                        Log.error(cause, "Authentication error");
                    }
                    return null;
                });

        return op;
    }
}
