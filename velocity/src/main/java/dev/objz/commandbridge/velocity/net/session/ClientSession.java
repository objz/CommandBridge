package dev.objz.commandbridge.velocity.net.session;

import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.security.AuthStatus;
import io.undertow.websockets.core.WebSocketChannel;

public final class ClientSession {
    private final WebSocketChannel ch;
    private volatile String id = "unknown";
    private volatile AuthStatus status = AuthStatus.AUTH_OK;
    private volatile Location location = Location.BACKEND;

    public ClientSession(WebSocketChannel ch, String clientId) {
        this.ch = ch;
        this.id = clientId;
    }

    public WebSocketChannel ch() {
        return ch;
    }

    public String id() {
        return id;
    }

    public AuthStatus status() {
        return status;
    }

    public void status(AuthStatus status) {
        this.status = status;
    }

    public Location location() {
        return location;
    }

    public void location(Location location) {
        this.location = location;

    }
}
