package dev.objz.commandbridge.api.message;

import dev.objz.commandbridge.api.channel.ChannelPayload;
import dev.objz.commandbridge.api.platform.Platform;

/**
 * Metadata accompanying a message received on a
 * {@link dev.objz.commandbridge.api.channel.MessageChannel}.
 *
 * <p>{@code channel} is the payload class token identifying the channel the message arrived on.
 * {@code from} identifies the server that sent the message.
 * {@code timestamp} is the Unix epoch millisecond value when the message was sent.
 *
 * @param <T> the payload type of the channel this message was received on;
 *     bounded by {@link dev.objz.commandbridge.api.channel.ChannelPayload}
 * @param channel the {@link Class} token of the payload type, identifying which channel
 *     this message was received on
 * @param from the {@link Platform.ServerTarget} identifying the server that sent the message
 * @param timestamp the time at which the message was sent, in milliseconds since the Unix epoch
 * @see dev.objz.commandbridge.api.message.MessageListener
 */
public record MessageContext<T extends ChannelPayload>(
        Class<T> channel,
        Platform.ServerTarget from,
        long timestamp
) {
}
