package dev.objz.commandbridge.backends.net.connection;

import dev.objz.commandbridge.api.platform.ConnectionState;

public enum ClientStatus {
    DISCONNECTED,
    CONNECTED,
    AUTH_OK,
    AUTH_FAILED;

    public ConnectionState toConnectionState() {
        return switch (this) {
            case DISCONNECTED -> ConnectionState.DISCONNECTED;
            case CONNECTED -> ConnectionState.CONNECTED;
            case AUTH_OK -> ConnectionState.AUTHENTICATED;
            case AUTH_FAILED -> ConnectionState.AUTH_FAILED;
        };
    }

    public static ClientStatus fromConnectionState(ConnectionState state) {
        return switch (state) {
            case DISCONNECTED, CONNECTING, RECONNECTING -> DISCONNECTED;
            case CONNECTED -> CONNECTED;
            case AUTHENTICATED -> AUTH_OK;
            case AUTH_FAILED -> AUTH_FAILED;
        };
    }
}
