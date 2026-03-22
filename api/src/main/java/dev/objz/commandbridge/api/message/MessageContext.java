package dev.objz.commandbridge.api.message;

import dev.objz.commandbridge.api.channel.ChannelPayload;
import dev.objz.commandbridge.api.platform.Platform;

/**
 * Metadata for a received message.
 *
 * @param channel the payload type of the channel the message was received on
 * @param from the server that sent the message
 * @param timestamp the time the message was sent
 * @param <T> the payload type
 */
public record MessageContext<T extends ChannelPayload>(
        Class<T> channel,
        Platform.ServerTarget from,
        long timestamp
) {
}
