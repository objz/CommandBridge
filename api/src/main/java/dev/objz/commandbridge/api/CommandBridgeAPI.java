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
 * Primary interface for interacting with the CommandBridge network from a third-party plugin.
 *
 * <p>Use {@link #channel(Class)} to obtain typed message channels for sending and receiving
 * payloads. {@link #server()} and {@link #connectionState()} expose the current server's identity
 * and connection status. {@link #connectedServers()}, {@link #playerLocator()},
 * {@link #onServerConnected(ServerEventListener)}, and
 * {@link #onServerDisconnected(ServerEventListener)} are available only on the Velocity proxy;
 * they return {@code Optional.empty()} on backend servers.
 *
 * <p>Obtain an instance via {@link CommandBridgeProvider#get()}. The following example shows
 * common usage patterns:
 *
 * <pre>{@code
 * CommandBridgeAPI api = CommandBridgeProvider.get();
 * MessageChannel<CommandPayload> channel = api.channel(CommandPayload.class);
 *
 * // Send a command to a backend
 * channel.to(List.of(Platform.backend("survival-1")))
 *        .send(new CommandPayload("say hello", RunAs.CONSOLE));
 *
 * // Broadcast to all connected servers
 * channel.toAll().send(new CommandPayload("say maintenance soon", RunAs.CONSOLE));
 *
 * // Subscribe to server connection events (proxy only)
 * api.onServerConnected(server ->
 *     System.out.println("Connected: " + server.id())
 * ).ifPresent(subscriptions::add);
 *
 * // React to connection state changes (all platforms)
 * api.onConnectionStateChanged(state -> {
 *     if (state.isActive()) {
 *         // ready to send
 *     }
 * });
 * }</pre>
 *
 * @see CommandBridgeProvider
 * @see dev.objz.commandbridge.api.channel.MessageChannel
 */
public interface CommandBridgeAPI {

    /**
     * Obtains a typed {@link dev.objz.commandbridge.api.channel.MessageChannel} for the given
     * payload type.
     *
     * @param <T> the payload type; must extend {@link dev.objz.commandbridge.api.channel.ChannelPayload}
     * @param type the {@link Class} token identifying the payload type; used for channel routing
     * @return the message channel for sending and receiving payloads of type {@code T}
     * @see dev.objz.commandbridge.api.channel.MessageChannel
     */
    <T extends ChannelPayload> MessageChannel<T> channel(Class<T> type);

    /**
     * Returns the identity of the server this API instance is running on.
     *
     * @return the {@link dev.objz.commandbridge.api.platform.Platform.ServerTarget} of the current
     *     server, containing its configured identifier and platform type
     */
    Platform.ServerTarget server();

    /**
     * Returns the current connection state of this server to the CommandBridge network.
     *
     * @return the current {@link ConnectionState}
     * @see ConnectionState#isActive()
     */
    ConnectionState connectionState();

    /**
     * Returns the identifiers of all servers currently connected to the bridge network.
     *
     * <p>This method is available only on the Velocity proxy. On backend servers, it returns
     * {@code Optional.empty()}.
     *
     * @return an {@link java.util.Optional} containing a snapshot of the connected server IDs if
     *     called on the Velocity proxy, or an empty {@code Optional} on backend servers
     * @see #onServerConnected(ServerEventListener)
     */
    Optional<Set<String>> connectedServers();

    /**
     * Returns the player location service for resolving which server a player is connected to.
     *
     * <p>This method is available only on the Velocity proxy. On backend servers, it returns
     * {@code Optional.empty()}.
     *
     * @return an {@link java.util.Optional} containing the {@link PlayerLocator} if called on the
     *     Velocity proxy, or an empty {@code Optional} on backend servers
     * @see dev.objz.commandbridge.api.platform.PlayerLocator#locate(java.util.UUID)
     */
    Optional<PlayerLocator> playerLocator();

    /**
     * Subscribes to server connection events.
     *
     * <p>This method is available only on the Velocity proxy. On backend servers, it returns
     * {@code Optional.empty()} and the listener is not registered.
     *
     * @param listener the listener to invoke when a backend server connects to the bridge network;
     *     must not be {@code null}
     * @return an {@link java.util.Optional} containing the {@link Subscription} on the proxy, or
     *     an empty {@code Optional} on backends
     * @see #onServerDisconnected(ServerEventListener)
     * @see dev.objz.commandbridge.api.message.Subscription#cancel()
     */
    Optional<Subscription> onServerConnected(ServerEventListener listener);

    /**
     * Subscribes to server disconnection events.
     *
     * <p>This method is available only on the Velocity proxy. On backend servers, it returns
     * {@code Optional.empty()} and the listener is not registered.
     *
     * @param listener the listener to invoke when a backend server disconnects from the bridge
     *     network; must not be {@code null}
     * @return an {@link java.util.Optional} containing the {@link Subscription} on the proxy, or
     *     an empty {@code Optional} on backends
     * @see #onServerConnected(ServerEventListener)
     * @see dev.objz.commandbridge.api.message.Subscription#cancel()
     */
    Optional<Subscription> onServerDisconnected(ServerEventListener listener);

    /**
     * Subscribes to connection state changes on this server.
     *
     * <p>Available on all platforms, unlike the server event methods. The listener is invoked on
     * every state transition.
     *
     * @param listener the consumer to call with the new {@link ConnectionState} on each transition;
     *     must not be {@code null}
     * @return a {@link Subscription} that can be cancelled to stop receiving state change events
     * @see ConnectionState
     */
    Subscription onConnectionStateChanged(Consumer<ConnectionState> listener);
}
