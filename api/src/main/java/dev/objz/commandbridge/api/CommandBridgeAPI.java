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

/**
 * Main entry point for interacting with the CommandBridge network.
 *
 * <p>Methods returning {@link java.util.Optional} indicate proxy-only features;
 * they return {@code Optional.empty()} on backend servers and a present value on the Velocity proxy.
 */
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

    /** @return the IDs of all currently connected servers, or empty if not available on this platform */
    Optional<Set<String>> connectedServers();

    /** @return the player location lookup service, or empty if not available on this platform */
    Optional<PlayerLocator> playerLocator();

    /**
     * Subscribes to server connection events.
     *
     * <p>Returns {@code Optional.empty()} on platforms where server connection events are not
     * available (e.g. backend servers). On the Velocity proxy, returns a present {@link Optional}
     * containing a {@link Subscription} that can be cancelled via {@link Subscription#cancel()}.
     * Use {@link java.util.Optional#ifPresent(java.util.function.Consumer)} to handle safely.
     *
     * @param listener the listener to call when a server connects
     * @return a present subscription handle on the proxy, or empty on backends
     */
    Optional<Subscription> onServerConnected(ServerEventListener listener);

    /**
     * Subscribes to server disconnection events.
     *
     * <p>Returns {@code Optional.empty()} on platforms where server disconnection events are not
     * available (e.g. backend servers). On the Velocity proxy, returns a present {@link Optional}
     * containing a {@link Subscription} that can be cancelled via {@link Subscription#cancel()}.
     * Use {@link java.util.Optional#ifPresent(java.util.function.Consumer)} to handle safely.
     *
     * @param listener the listener to call when a server disconnects
     * @return a present subscription handle on the proxy, or empty on backends
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
