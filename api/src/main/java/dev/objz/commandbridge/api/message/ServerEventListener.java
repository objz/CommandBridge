package dev.objz.commandbridge.api.message;

import dev.objz.commandbridge.api.platform.Platform;

/**
 * Callback invoked when a server connects to or disconnects from the
 * CommandBridge network.
 *
 * <p>
 * This is a {@code @FunctionalInterface} and may be used as a lambda
 * expression. Register listeners via
 * {@link dev.objz.commandbridge.api.CommandBridgeAPI#onServerConnected(ServerEventListener)}
 * and
 * {@link dev.objz.commandbridge.api.CommandBridgeAPI#onServerDisconnected(ServerEventListener)}.
 * Both methods return an {@link java.util.Optional} wrapping a
 * {@link dev.objz.commandbridge.api.message.Subscription} on the Velocity
 * proxy, and {@code Optional.empty()} on backend servers.
 *
 * <p>
 * The listener receives the server that triggered the event:
 *
 * <pre>{@code
 * server -> Log.info("Server {}: {}", server.id(), server.type())
 * }</pre>
 *
 * @see dev.objz.commandbridge.api.CommandBridgeAPI#onServerConnected(ServerEventListener)
 * @see dev.objz.commandbridge.api.CommandBridgeAPI#onServerDisconnected(ServerEventListener)
 * @see dev.objz.commandbridge.api.message.Subscription
 */
@FunctionalInterface
public interface ServerEventListener {

    /**
     * Invoked when a server connects or disconnects from the bridge network.
     *
     * @param server the server involved in the event, providing its unique
     *               identifier via
     *               {@link dev.objz.commandbridge.api.platform.Platform.ServerTarget#id()}
     *               and its
     *               platform type via
     *               {@link dev.objz.commandbridge.api.platform.Platform.ServerTarget#type()}
     */
    void accept(Platform.ServerTarget server);
}
