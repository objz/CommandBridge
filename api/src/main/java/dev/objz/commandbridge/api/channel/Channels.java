package dev.objz.commandbridge.api.channel;

import dev.objz.commandbridge.api.channel.command.CommandChannelType;

public final class Channels {

    public static final CommandChannelType COMMAND = new CommandChannelType();

    private Channels() {
        throw new UnsupportedOperationException("Channels can't be instanced");
    }
}
