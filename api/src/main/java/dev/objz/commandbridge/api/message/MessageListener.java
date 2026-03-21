package dev.objz.commandbridge.api.message;

import dev.objz.commandbridge.api.channel.ChannelPayload;

@FunctionalInterface
public interface MessageListener<T extends ChannelPayload> {
    void accept(MessageContext<T> ctx, T payload);
}
