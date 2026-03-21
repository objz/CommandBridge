package dev.objz.commandbridge.api.channel.command;

import dev.objz.commandbridge.api.channel.ChannelType;

public final class CommandChannelType extends ChannelType<CommandPayload, CommandChannel> {

    public CommandChannelType() {
        super(CommandPayload.class);
    }
}
