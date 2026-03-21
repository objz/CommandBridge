package dev.objz.commandbridge.api.message;

import dev.objz.commandbridge.api.channel.ChannelPayload;
import dev.objz.commandbridge.api.channel.ChannelType;
import dev.objz.commandbridge.api.channel.MessageChannel;
import dev.objz.commandbridge.api.platform.Platform;

public record MessageContext<T extends ChannelPayload>(
        ChannelType<T, ? extends MessageChannel<T>> channel,
        Platform.ServerTarget from,
        long timestamp
) {
}
