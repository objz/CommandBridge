package dev.objz.commandbridge.api.message;

import dev.objz.commandbridge.api.channel.ChannelPayload;

/**
 * Handles incoming messages for a specific channel.
 *
 * @param <T> the payload type
 */
@FunctionalInterface
public interface MessageListener<T extends ChannelPayload> {
    /**
     * Invoked when a message is received.
     *
     * @param ctx the message metadata
     * @param payload the received data
     */
    void accept(MessageContext<T> ctx, T payload);
}
