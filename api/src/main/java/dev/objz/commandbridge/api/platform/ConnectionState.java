package dev.objz.commandbridge.api.platform;

/** Represents the network connection status between a server and the bridge. */
public enum ConnectionState {

    /** No connection established. */
    DISCONNECTED,

    /** Attempting to establish a socket connection. */
    CONNECTING,

    /** Socket connected, awaiting authentication. */
    CONNECTED,

    /** Fully connected and authorized to send/receive messages. */
    AUTHENTICATED,

    /** Connection lost, attempting to re-establish. */
    RECONNECTING,

    /** Authentication failed; no further attempts will be made. */
    AUTH_FAILED;

    /** @return true if the connection is established, even if not yet authenticated */
    public boolean isActive() {
        return this == CONNECTED || this == AUTHENTICATED;
    }

    /** @return true if the connection is ready for message transport */
    public boolean canSend() {
        return this == AUTHENTICATED;
    }

    /** @return true if the connection reached a non-recoverable error state */
    public boolean isTerminal() {
        return this == AUTH_FAILED;
    }
}
