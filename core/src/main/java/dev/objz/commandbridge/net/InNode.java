package dev.objz.commandbridge.net;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public final class InNode {

    private final Map<MessageType, InboundHandler> handlers;
    private Predicate<Envelope> inboundTap;
    private volatile BiPredicate<Endpoint, Envelope> gate;
    private BiFunction<Endpoint, Envelope, SendOperation> sendOperationFactory;

    public InNode() {
        this.handlers = Collections.synchronizedMap(new EnumMap<>(MessageType.class));
    }

    public void setInboundTap(Predicate<Envelope> tap) {
        this.inboundTap = tap;
    }

    /** Decides per message whether it may be handled at all, before the tap; e.g. nothing but auth until authenticated. */
    public void setGate(BiPredicate<Endpoint, Envelope> gate) {
        this.gate = gate;
    }

    public InNode setSendOperationFactory(BiFunction<Endpoint, Envelope, SendOperation> factory) {
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

    public void onText(Endpoint endpoint, String text) {
        final Envelope env;
        try {
            env = Envelope.MAPPER.readValue(text, Envelope.class);
        } catch (Exception e) {
            String source = endpoint != null ? endpoint.describe() : "unknown";
            Log.warn("Bad JSON from {}: {}", source, e.getMessage());
            return;
        }

        Log.debug("Inbound message type={} id={} from={}", env.type(), env.id(), env.from());

        if (gate != null && !gate.test(endpoint, env)) {
            Log.debug("Dropped {} from {}: not authenticated", env.type(), endpoint != null ? endpoint.describe() : "unknown");
            return;
        }

        if (inboundTap != null) {
            try {
                if (inboundTap.test(env)) {
                    Log.debug("Inbound message type={} id={} consumed by awaiter", env.type(), env.id());
                    return;
                }
            } catch (Exception e) {
                Log.warn("Inbound tap threw exception for type {}: {}", env.type(), e.getMessage());
            }
        }

        var handler = handlers.get(env.type());
        if (handler == null) {
            Log.error("Unhandled message type: {}", env.type());
            return;
        }

        try {
            handler.accept(endpoint, env);
        } catch (Exception ex) {
            Log.error(ex, "Handler failure for type {}", env.type());
        }
    }
}
