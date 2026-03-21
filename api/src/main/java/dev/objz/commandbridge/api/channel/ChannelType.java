package dev.objz.commandbridge.api.channel;

import java.util.Objects;

/**
 * Identity and type information for a {@link MessageChannel}.
 *
 * @param <T> the payload type
 * @param <C> the channel interface type
 */
public abstract class ChannelType<T extends ChannelPayload, C extends MessageChannel<T>> {

    private final Class<T> type;

    protected ChannelType(Class<T> type) {
        this.type = Objects.requireNonNull(type);
    }

    /** @return the class of the payload handled by this channel */
    public Class<T> type() {
        return type;
    }
}
