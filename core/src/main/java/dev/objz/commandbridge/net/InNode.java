package dev.objz.commandbridge.net;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class InNode {

    private final Map<MessageType, InboundHandler> handlers;
    private Predicate<Envelope> inboundTap;
    private BiFunction<WebSocketChannel, Envelope, SendOperation> sendOperationFactory;

    public InNode() {
        this.handlers = new EnumMap<>(MessageType.class);
    }

    public void setInboundTap(Predicate<Envelope> tap) {
        this.inboundTap = tap;
    }

    public InNode setSendOperationFactory(BiFunction<WebSocketChannel, Envelope, SendOperation> factory) {
        this.sendOperationFactory = factory;
        return this;
    }

    public InNode register(MessageType type, InboundHandler handler) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(handler);
        handler.setSendOperationFactory(sendOperationFactory);
        handlers.put(type, handler);
        return this;
    }

    public void onText(WebSocketChannel ch, String text) {
        final Envelope env;
        try {
            env = Envelope.MAPPER.readValue(text, Envelope.class);
        } catch (Exception e) {
            Log.warn("Bad JSON from {}: {}", ch.getSourceAddress(), e.getMessage());
            return;
        }

        if (inboundTap != null) {
            try {
                if (inboundTap.test(env))
                    return;
            } catch (Exception ignored) {
            }
        }

        var handler = handlers.get(env.type());
        if (handler == null) {
            Log.error("Unhandled message type: {}", env.type());
            return;
        }

        try {
            handler.accept(ch, env);
        } catch (Exception ex) {
            Log.error(ex, "Handler failure for type {}", env.type());
        }
    }
}
