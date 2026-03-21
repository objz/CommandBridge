package dev.objz.commandbridge.api.channel;

import dev.objz.commandbridge.api.channel.command.CommandChannelType;

/** Registry of built-in {@link ChannelType}s. */
public final class Channels {

    /** The default channel for executing commands. */
    public static final CommandChannelType COMMAND = new CommandChannelType();

    private Channels() {
        throw new UnsupportedOperationException("Channels can't be instanced");
    }
}
