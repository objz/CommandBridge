package dev.objz.commandbridge.api.message;

import dev.objz.commandbridge.api.channel.ChannelPayload;
import dev.objz.commandbridge.api.channel.ChannelType;
import dev.objz.commandbridge.api.channel.MessageChannel;
import dev.objz.commandbridge.api.platform.Platform;

/**
 * Metadata for a received message.
 *
 * @param channel the channel the message was received on
 * @param from the server that sent the message
 * @param timestamp the time the message was sent
 * @param <T> the payload type
 */
public record MessageContext<T extends ChannelPayload>(
        ChannelType<T, ? extends MessageChannel<T>> channel,
        Platform.ServerTarget from,
        long timestamp
) {
}
