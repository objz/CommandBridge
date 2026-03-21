package dev.objz.commandbridge.api.platform;

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
}
