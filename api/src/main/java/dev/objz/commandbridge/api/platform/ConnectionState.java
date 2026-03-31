package dev.objz.commandbridge.api.platform;

/**
 * Represents the network connection status between a server and the
 * CommandBridge network.
 *
 * <p>
 * A server progresses through states in the following normal lifecycle: it
 * begins in
 * {@code DISCONNECTED}, advances to {@code CONNECTING} while the socket is
 * being established,
 * then to {@code CONNECTED} once the socket is open (but before authentication
 * completes),
 * and finally to {@code AUTHENTICATED} when the server is fully operational and
 * ready to
 * exchange messages. If the connection drops unexpectedly from
 * {@code AUTHENTICATED} or
 * {@code CONNECTED}, the state transitions to {@code RECONNECTING} and the
 * client attempts
 * to re-establish the connection. If authentication fails at any point, the
 * state becomes
 * {@code AUTH_FAILED} and no further connection attempts are made.
 *
 * @see #isActive()
 * @see dev.objz.commandbridge.api.CommandBridgeAPI#connectionState()
 */
public enum ConnectionState {

    /**
     * No connection has been established.
     *
     * <p>
     * This is the initial state before any connection attempt has been made.
     */
    DISCONNECTED,

    /**
     * A socket connection is being established.
     *
     * <p>
     * This state precedes {@code CONNECTED} and indicates that the client is
     * actively
     * attempting to open a socket to the bridge network.
     */
    CONNECTING,

    /**
     * The socket connection is established; awaiting authentication.
     *
     * <p>
     * This is an intermediate state. The socket is open but the handshake has not
     * yet
     * completed, so messages cannot be sent or received until the state advances to
     * {@link #AUTHENTICATED}.
     */
    CONNECTED,

    /**
     * Fully connected and authenticated; messages may be sent and received.
     *
     * <p>
     * This is the only state in which {@link #isActive()} returns {@code true}. All
     * message channels are operational while the server remains in this state.
     */
    AUTHENTICATED,

    /**
     * The connection was lost and a reconnection attempt is in progress.
     *
     * <p>
     * This state occurs after an unexpected disconnect from {@code AUTHENTICATED}
     * or
     * {@code CONNECTED}. The client will attempt to re-establish the connection and
     * progress back through {@code CONNECTING} and {@code CONNECTED}.
     */
    RECONNECTING,

    /**
     * Authentication failed; no further connection attempts will be made.
     *
     * <p>
     * If the server reaches this state, verify that the configured secret key
     * matches
     * on both the Velocity proxy and the backend server.
     */
    AUTH_FAILED;

    /**
     * Returns whether the connection is established and ready for message
     * transport.
     *
     * @return {@code true} if and only if the current state is
     *         {@link #AUTHENTICATED},
     *         {@code false} otherwise
     */
    public boolean isActive() {
        return this == AUTHENTICATED;
    }
}
