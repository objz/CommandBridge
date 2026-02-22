package dev.objz.commandbridge.net;

import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.function.BiFunction;

public abstract class InboundHandler {

    private BiFunction<WebSocketChannel, Envelope, SendOperation> sendOperationFactory;

    public void setSendOperationFactory(BiFunction<WebSocketChannel, Envelope, SendOperation> factory) {
        this.sendOperationFactory = factory;
    }

    /**
     * Handles an inbound message
     * 
     * @param ch  The WebSocket channel
     * @param env The received envelope
     */
    public abstract void accept(WebSocketChannel ch, Envelope env);

    /**
     * Sends a reply to the original request
     * 
     * @param ch        The WebSocket channel
     * @param request   The original request envelope
     * @param replyType The message type for the reply
     * @param payload   The payload object
     * @return SendOperation
     */
    protected SendOperation reply(WebSocketChannel ch, Envelope request, MessageType replyType, Object payload) {
        if (sendOperationFactory == null) {
            throw new IllegalStateException("SendOperation factory not configured");
        }
        var payloadNode = Envelope.MAPPER.valueToTree(payload);
        Envelope replyEnv = Envelope.reply(request, replyType, request.to(), payloadNode);
        return sendOperationFactory.apply(ch, replyEnv);
    }
}
