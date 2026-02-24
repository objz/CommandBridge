package dev.objz.commandbridge.net;

import dev.objz.commandbridge.net.proto.Envelope;

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
    private BiFunction<Endpoint, Envelope, SendOperation> endpointSendFactory;

    public void setSendOperationFactory(Function<Envelope, SendOperation> factory) {
        this.sendOperationFactory = factory;
    }

    public void setEndpointSendFactory(BiFunction<Endpoint, Envelope, SendOperation> factory) {
        this.endpointSendFactory = factory;
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
            throw new IllegalStateException("Default send factory not configured");
        }
        return sendOperationFactory.apply(envelope);
    }

    /**
     * @param endpoint The endpoint to send to
     * @param envelope The envelope to send
     * @return SendOperation
     */
    protected SendOperation send(Endpoint endpoint, Envelope envelope) {
        if (endpointSendFactory == null) {
            throw new IllegalStateException("Endpoint send factory not configured");
        }
        return endpointSendFactory.apply(endpoint, envelope);
    }
}
