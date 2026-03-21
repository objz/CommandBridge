package dev.objz.commandbridge.api.channel;

import java.util.Objects;

public abstract class ChannelType<T extends ChannelPayload, C extends MessageChannel<T>> {

    private final Class<T> type;

    protected ChannelType(Class<T> type) {
        this.type = Objects.requireNonNull(type);
    }

    public Class<T> type() {
        return type;
    }
}
