package dev.objz.commandbridge.net;

import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;

import java.util.function.BiFunction;

public abstract class InboundHandler {

    private BiFunction<Endpoint, Envelope, SendOperation> sendOperationFactory;

    public void setSendOperationFactory(BiFunction<Endpoint, Envelope, SendOperation> factory) {
        this.sendOperationFactory = factory;
    }

    /**
     * Handles an inbound message
     * 
     * @param endpoint The transport endpoint
     * @param env The received envelope
     */
    public abstract void accept(Endpoint endpoint, Envelope env);

    /**
     * Sends a reply to the original request
     * 
     * @param endpoint  The transport endpoint
     * @param request   The original request envelope
     * @param replyType The message type for the reply
     * @param payload   The payload object
     * @return SendOperation
     */
    protected SendOperation reply(Endpoint endpoint, Envelope request, MessageType replyType, Object payload) {
        if (sendOperationFactory == null) {
            throw new IllegalStateException("Inbound send factory not configured");
        }
        var payloadNode = Envelope.MAPPER.valueToTree(payload);
        Envelope replyEnv = Envelope.reply(request, replyType, request.to(), payloadNode);
        return sendOperationFactory.apply(endpoint, replyEnv);
    }
}
