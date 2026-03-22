package dev.objz.commandbridge.net;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.proto.MessageType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import dev.objz.commandbridge.net.proto.Envelope;

public final class OutNode {

    private final Map<MessageType, OutboundHandler<?>> handlers;
    private Function<Envelope, SendOperation> sendOperationFactory;
    private BiFunction<Endpoint, Envelope, SendOperation> endpointSendFactory;
    private String clientId;
    private String serverId;

    public OutNode() {
        this.handlers = Collections.synchronizedMap(new EnumMap<>(MessageType.class));
    }

    public OutNode setSendOperationFactory(Function<Envelope, SendOperation> factory) {
        this.sendOperationFactory = factory;
        return this;
    }

    public OutNode setEndpointSendFactory(
            BiFunction<Endpoint, Envelope, SendOperation> factory) {
        this.endpointSendFactory = factory;
        return this;
    }

    public OutNode setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public OutNode setServerId(String serverId) {
        this.serverId = serverId;
        return this;
    }

    public <T> OutNode register(MessageType type, OutboundHandler<T> handler) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(handler);
        handler.setSendOperationFactory(sendOperationFactory);
        handler.setEndpointSendFactory(endpointSendFactory);
        handler.setClientId(clientId);
        handler.setServerId(serverId);
        handlers.put(type, handler);
        return this;
    }

    /**
     * @param type    The message type to send
     * @param context The context containing data for building the message
     * @return SendOperation
     */
    @SuppressWarnings("unchecked")
    public <T> SendOperation send(MessageType type, T context) {
        if (type == null) {
            throw new IllegalArgumentException("MessageType cannot be null");
        }

        var handler = handlers.get(type);
        if (handler == null) {
            Log.warn("No OutboundHandler registered for: {}", type);
            throw new IllegalStateException("No OutboundHandler registered for " + type);
        }

        if (sendOperationFactory == null && endpointSendFactory == null) {
            throw new IllegalStateException(
                    "Send factory not configured. Call setSendOperationFactory() or setEndpointSendFactory() first.");
        }

        Log.debug("Outbound message type={}", type);
        try {
            OutboundHandler<T> typedHandler = (OutboundHandler<T>) handler;
            return typedHandler.accept(context);
        } catch (Exception ex) {
            Log.error(ex, "Outbound handler threw for {}", type);
            throw new RuntimeException("Failed to send message for " + type, ex);
        }
    }

    /**
     * @param type The message type to send
     * @return SendOperation
     */
    public SendOperation send(MessageType type) {
        return send(type, null);
    }
}
