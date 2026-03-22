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

    /** @return true if the connection is established and ready for message transport */
    public boolean isActive() {
        return this == AUTHENTICATED;
    }
}
