package dev.objz.commandbridge.api;

import dev.objz.commandbridge.api.channel.ChannelPayload;
import dev.objz.commandbridge.api.channel.MessageChannel;
import dev.objz.commandbridge.api.message.ServerEventListener;
import dev.objz.commandbridge.api.message.Subscription;
import dev.objz.commandbridge.api.platform.ConnectionState;
import dev.objz.commandbridge.api.platform.Platform;
import dev.objz.commandbridge.api.platform.PlayerLocator;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/** Main entry point for interacting with the CommandBridge network. */
public interface CommandBridgeAPI {

    /**
     * Obtains a {@link MessageChannel} for the given payload type.
     *
     * @param type the payload class that identifies the channel
     * @param <T> the payload type
     * @return the message channel
     */
    <T extends ChannelPayload> MessageChannel<T> channel(Class<T> type);

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
    Optional<Subscription> onServerConnected(ServerEventListener listener);

    /**
     * Subscribes to server disconnection events.
     *
     * @param listener the listener to call when a server disconnects
     * @return a subscription handle to cancel the listener
     */
    Optional<Subscription> onServerDisconnected(ServerEventListener listener);

    /**
     * Subscribes to connection state changes.
     *
     * @param listener the listener to call when the state changes
     * @return a subscription handle to cancel the listener
     */
    Subscription onConnectionStateChanged(Consumer<ConnectionState> listener);
}
