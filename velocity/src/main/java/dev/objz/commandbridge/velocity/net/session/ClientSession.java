package dev.objz.commandbridge.velocity.net.session;

import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.security.AuthStatus;

public final class ClientSession {
    private volatile Endpoint endpoint;
    private volatile String id;
    private volatile AuthStatus status = AuthStatus.AUTH_FAIL;
    private volatile Location location = Location.BACKEND;

    public ClientSession(Endpoint endpoint, String clientId) {
        this.endpoint = endpoint;
        this.id = clientId;
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

    public Endpoint endpoint() {
        return endpoint;
    }

    public void endpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public Location location() {
        return location;
    }

    public void location(Location location) {
        this.location = location;

    }
}
