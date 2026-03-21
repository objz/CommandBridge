package dev.objz.commandbridge.api.channel.command;

import dev.objz.commandbridge.api.channel.ChannelType;

/** Identity for the {@link CommandChannel}. */
public final class CommandChannelType extends ChannelType<CommandPayload, CommandChannel> {

    /** Creates a new command channel type. */
    public CommandChannelType() {
        super(CommandPayload.class);
    }
}
