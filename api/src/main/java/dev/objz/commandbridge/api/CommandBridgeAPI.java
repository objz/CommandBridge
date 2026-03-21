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
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Main entry point for interacting with the CommandBridge network. */
public interface CommandBridgeAPI {

    /**
     * Obtains a {@link MessageChannel} for the given {@link ChannelType}.
     *
     * @param type the channel type identity
     * @param <T> the payload type
     * @param <C> the channel interface type
     * @return the message channel
     */
    <T extends ChannelPayload, C extends MessageChannel<T>> C channel(ChannelType<T, C> type);

    /**
     * Broadcasts a payload to all connected servers on a specific channel.
     *
     * @param channel the channel to broadcast on
     * @param payload the data to send
     * @return a future that completes when the broadcast is dispatched
     */
    <P extends ChannelPayload> CompletableFuture<Void> broadcast(MessageChannel<P> channel, P payload);

    /** @return the identity of the current server */
    Platform.ServerTarget server();

    /** @return the current connection state to the bridge network */
    ConnectionState connectionState();

    /** @return the IDs of all currently connected servers, if available */
    Optional<Set<String>> connectedServers();

    /** @return the player location lookup service, if available */
    Optional<PlayerLocator> playerLocator();

    /**
     * Subscribes to server connection events.
     *
     * @param listener the listener to call when a server connects
     * @return a subscription handle to cancel the listener
     */
    Subscription onServerConnected(ServerEventListener listener);

    /**
     * Subscribes to server disconnection events.
     *
     * @param listener the listener to call when a server disconnects
     * @return a subscription handle to cancel the listener
     */
    Subscription onServerDisconnected(ServerEventListener listener);

    /**
     * Subscribes to connection state changes.
     *
     * @param listener the listener to call when the state changes
     * @return a subscription handle to cancel the listener
     */
    Subscription onConnectionStateChanged(Consumer<ConnectionState> listener);
}
