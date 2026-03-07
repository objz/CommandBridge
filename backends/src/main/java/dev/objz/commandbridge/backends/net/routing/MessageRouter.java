package dev.objz.commandbridge.backends.net.routing;

import dev.objz.commandbridge.backends.net.connection.ConnectionState;
import dev.objz.commandbridge.backends.net.in.PingHandler;
import dev.objz.commandbridge.backends.net.out.AuthRequest;
import dev.objz.commandbridge.backends.net.out.InvokedCommandEvent;
import dev.objz.commandbridge.backends.net.out.PlayerListEvent;
import dev.objz.commandbridge.backends.net.out.PlayerUpdateEvent;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.InNode;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.ResponseAwaiter;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.endpoints.WsEndpoint;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.security.AuthService;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public final class MessageRouter {
    private final InNode inNode;
    private final OutNode<Object> outNode;
    private final ResponseAwaiter awaiter;
    private final AtomicReference<ConnectionState> stateRef;
    private final Runnable reconnectCallback;
    private final String secret;
    private volatile Location location;

    public MessageRouter(
            InNode inNode,
            OutNode<Object> outNode,
            ResponseAwaiter awaiter,
            AtomicReference<ConnectionState> stateRef,
            Runnable reconnectCallback,
            String secret,
            Location location) {
        this.inNode = inNode;
        this.outNode = outNode;
        this.awaiter = awaiter;
        this.stateRef = stateRef;
        this.reconnectCallback = reconnectCallback;
        this.secret = secret;
        this.location = location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setupChannel(WebSocketChannel channel) {
        var endpoint = new WsEndpoint(channel);

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

        channel.getReceiveSetter().set(new AbstractReceiveListener() {
            @Override
            protected void onFullTextMessage(WebSocketChannel ch, BufferedTextMessage message) {
                try {
                    inNode.onText(endpoint, message.getData());
                } catch (Throwable t) {
                    Log.error(t, "Inbound message handling failed");
                }
            }

            @Override
            protected void onFullCloseMessage(WebSocketChannel ch, BufferedBinaryMessage message) {
                try {
                    var data = message.getData();
                    data.close();
                } catch (Throwable t) {
                    Log.debug("Failed to release close frame buffer: {}", t.getMessage());
                }
            }

            @Override
            protected void onClose(WebSocketChannel ch, StreamSourceFrameChannel frameChannel) {
                try {
                    super.onClose(ch, frameChannel);
                } catch (IOException e) {
                    Log.debug("Failed to buffer close frame: {}", e.getMessage());
                }

                try {
                    Log.warn("Connection lost - attempting to reconnect");
                    stateRef.set(ConnectionState.RECONNECTING);
                    reconnectCallback.run();
                } catch (Throwable ignore) {
                }
            }
        });

        channel.resumeReceives();

        outNode.register(MessageType.AUTH_REQUEST, new AuthRequest(new AuthService(secret), location));
        outNode.register(MessageType.INVOKED_COMMAND, new InvokedCommandEvent());
        outNode.register(MessageType.PLAYER_LIST, new PlayerListEvent());
        outNode.register(MessageType.PLAYER_JOIN, new PlayerUpdateEvent(MessageType.PLAYER_JOIN));
        outNode.register(MessageType.PLAYER_LEAVE, new PlayerUpdateEvent(MessageType.PLAYER_LEAVE));

        inNode.register(MessageType.PING, new PingHandler());
    }

    public void clearTap() {
        if (inNode != null) {
            inNode.setInboundTap(null);
        }
    }
}
