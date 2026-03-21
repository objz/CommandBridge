package dev.objz.commandbridge.api;

import dev.objz.commandbridge.api.channel.ChannelPayload;
import dev.objz.commandbridge.api.channel.ChannelType;
import dev.objz.commandbridge.api.channel.MessageChannel;
import dev.objz.commandbridge.api.message.ServerEventListener;
import dev.objz.commandbridge.api.message.Subscription;
import dev.objz.commandbridge.api.platform.ConnectionState;
import dev.objz.commandbridge.api.platform.Platform;
import dev.objz.commandbridge.api.platform.PlayerLocator;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public interface CommandBridgeAPI {

    <T extends ChannelPayload, C extends MessageChannel<T>> C channel(ChannelType<T, C> type);

    <P extends ChannelPayload> void broadcast(MessageChannel<P> channel, P payload);

    Platform.ServerTarget server();

    ConnectionState connectionState();

    Optional<Set<String>> connectedServers();

    Optional<PlayerLocator> playerLocator();

    Subscription onServerConnected(ServerEventListener listener);

    Subscription onServerDisconnected(ServerEventListener listener);

    Subscription onConnectionStateChanged(Consumer<ConnectionState> listener);
}
