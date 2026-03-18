package dev.objz.commandbridge.backends.net.routing;

import dev.objz.commandbridge.backends.net.connection.ConnectionState;
import dev.objz.commandbridge.backends.net.in.PingHandler;
import dev.objz.commandbridge.backends.net.out.AuthRequest;
import dev.objz.commandbridge.backends.net.out.InvokedCommandEvent;
import dev.objz.commandbridge.backends.net.out.PlayerListEvent;
import dev.objz.commandbridge.backends.net.out.PlayerUpdateEvent;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.InNode;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.ResponseAwaiter;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.security.AuthService;

import java.util.concurrent.atomic.AtomicReference;

public final class RedisMessageRouter {
    private final InNode inNode;
    private final OutNode outNode;
    private final ResponseAwaiter awaiter;
    private final AtomicReference<ConnectionState> stateRef;
    private final String secret;
    private volatile Location location;

    public RedisMessageRouter(
            InNode inNode,
            OutNode outNode,
            ResponseAwaiter awaiter,
            AtomicReference<ConnectionState> stateRef,
            String secret,
            Location location) {
        this.inNode = inNode;
        this.outNode = outNode;
        this.awaiter = awaiter;
        this.stateRef = stateRef;
        this.secret = secret;
        this.location = location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setupEndpoint(Endpoint endpoint) {
        inNode.setSendOperationFactory((ep, envelope) -> new SendOperation(ep, envelope, awaiter));
        outNode.setSendOperationFactory(envelope -> new SendOperation(endpoint, envelope, awaiter));

        inNode.setInboundTap(env -> {
            boolean matched = false;
            try {
                matched = awaiter.signal(env);
            } catch (Exception ignore) {
            }

            ConnectionState state = stateRef.get();
            if (state != ConnectionState.AUTHENTICATED) {
                switch (env.type()) {
                    case AUTH_OK:
                    case AUTH_FAIL:
                        return matched;
                    default:
                        Log.warn("Dropping {} while unauthenticated", env.type());
                        return true;
                }
            }
            return matched;
        });

        outNode.register(MessageType.AUTH_REQUEST, new AuthRequest(new AuthService(secret), location));
        outNode.register(MessageType.INVOKED_COMMAND, new InvokedCommandEvent());
        outNode.register(MessageType.PLAYER_LIST, new PlayerListEvent());
        outNode.register(MessageType.PLAYER_JOIN, new PlayerUpdateEvent(MessageType.PLAYER_JOIN));
        outNode.register(MessageType.PLAYER_LEAVE, new PlayerUpdateEvent(MessageType.PLAYER_LEAVE));
        inNode.register(MessageType.PING, new PingHandler());
    }

    public void onText(Endpoint endpoint, String text) {
        inNode.onText(endpoint, text);
    }

    public void clearTap() {
        inNode.setInboundTap(null);
    }
}
