package dev.objz.commandbridge.net.channel;

import dev.objz.commandbridge.api.channel.command.CommandChannel;
import dev.objz.commandbridge.api.channel.command.CommandChannelType;
import dev.objz.commandbridge.api.channel.command.CommandPayload;

public final class CommandMessageChannel extends PluginMessageChannel<CommandPayload> implements CommandChannel {

    public CommandMessageChannel(CommandChannelType type,
            SendTransport sendTransport,
            RequestTransport requestTransport,
            ListenerRegistrar<CommandPayload> listenerRegistrar) {
        super(type, sendTransport, requestTransport, listenerRegistrar);
    }
}
