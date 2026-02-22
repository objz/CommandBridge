package dev.objz.commandbridge.net;

import dev.objz.commandbridge.net.proto.Envelope;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @param <T> The context type that contains data for building the outbound
 *            message
 */
public abstract class OutboundHandler<T> {

    protected String clientId;
    protected String serverId;
    private Function<Envelope, SendOperation> sendOperationFactory;
    private BiFunction<WebSocketChannel, Envelope, SendOperation> channelSendOperationFactory;

    public void setSendOperationFactory(Function<Envelope, SendOperation> factory) {
        this.sendOperationFactory = factory;
    }

    public void setChannelSendOperationFactory(BiFunction<WebSocketChannel, Envelope, SendOperation> factory) {
        this.channelSendOperationFactory = factory;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    /**
     * @param context The context containing data for building the message
     * @return SendOperation
     */
    public abstract SendOperation accept(T context);

    /**
     * @param envelope The envelope to send
     * @return SendOperation
     */
    protected SendOperation send(Envelope envelope) {
        if (sendOperationFactory == null) {
            throw new IllegalStateException("SendOperation factory not configured");
        }
        return sendOperationFactory.apply(envelope);
    }

    /**
     * @param channel  The WebSocket channel to send to
     * @param envelope The envelope to send
     * @return SendOperation
     */
    protected SendOperation send(WebSocketChannel channel, Envelope envelope) {
        if (channelSendOperationFactory == null) {
            throw new IllegalStateException("Channel SendOperation factory not configured");
        }
        return channelSendOperationFactory.apply(channel, envelope);
    }
}
