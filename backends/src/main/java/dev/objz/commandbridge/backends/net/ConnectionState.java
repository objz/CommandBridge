package dev.objz.commandbridge.backends.net;

public enum ConnectionState {

    DISCONNECTED,

    CONNECTING,

    CONNECTED,

    AUTHENTICATED,

    RECONNECTING,

    AUTH_FAILED;

    public boolean isActive() {
        return this == CONNECTED || this == AUTHENTICATED;
    }

    public boolean canSend() {
        return this == AUTHENTICATED;
    }

    public boolean isTerminal() {
        return this == AUTH_FAILED;
    }

    public static ConnectionState fromClientStatus(ClientStatus status) {
        return switch (status) {
            case DISCONNECTED -> DISCONNECTED;
            case CONNECTED -> CONNECTED;
            case AUTH_OK -> AUTHENTICATED;
            case AUTH_FAILED -> AUTH_FAILED;
        };
    }

    public ClientStatus toClientStatus() {
        return switch (this) {
            case DISCONNECTED, CONNECTING, RECONNECTING -> ClientStatus.DISCONNECTED;
            case CONNECTED -> ClientStatus.CONNECTED;
            case AUTHENTICATED -> ClientStatus.AUTH_OK;
            case AUTH_FAILED -> ClientStatus.AUTH_FAILED;
        };
    }
}
