package dev.objz.commandbridge.velocity.net.session;

import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.security.AuthStatus;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionHub implements Iterable<ClientSession> {

    private final ConcurrentHashMap<String, ClientSession> clientsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Endpoint, ClientSession> clientsByEndpoint = new ConcurrentHashMap<>();

    public ClientSession add(String clientId, Endpoint endpoint) {
        Objects.requireNonNull(endpoint);

        if (clientId == null || clientId.isBlank())
            clientId = "unknown-" + UUID.randomUUID();

        var s = new ClientSession(endpoint, clientId);
        var previous = clientsById.put(clientId, s);
        if (previous != null && previous.endpoint() != null) {
            clientsByEndpoint.remove(previous.endpoint());
        }
        clientsByEndpoint.put(endpoint, s);
        return s;
    }

    public Optional<ClientSession> get(String clientId) {
        if (clientId == null || clientId.isBlank())
            return Optional.empty();
        return Optional.ofNullable(clientsById.get(clientId));
    }

    public ClientSession get(Endpoint endpoint) {
        if (endpoint == null)
            return null;
        return clientsByEndpoint.get(endpoint);
    }

    public ClientSession remove(String clientId) {
        if (clientId == null)
            return null;
        var removed = clientsById.remove(clientId);
        if (removed != null && removed.endpoint() != null) {
            clientsByEndpoint.remove(removed.endpoint());
        }
        return removed;
    }

    public ClientSession remove(Endpoint endpoint) {
        if (endpoint == null)
            return null;

        var removed = clientsByEndpoint.remove(endpoint);
        if (removed != null && removed.id() != null) {
            clientsById.remove(removed.id(), removed);
        }
        return removed;
    }

    public void clear() {
        clientsById.clear();
        clientsByEndpoint.clear();
    }

    public int size() {
        return clientsById.size();
    }

    @Override
    public Iterator<ClientSession> iterator() {
        return clientsById.values().iterator();
    }

    public Optional<ClientSession> findSession(String id, Location location) {
        if (id == null || location == null)
            return Optional.empty();

        var session = clientsById.get(id);
        if (session == null)
            return Optional.empty();

        if (!id.equals(session.id()))
            return Optional.empty();

        if (session.location() != location)
            return Optional.empty();

        if (session.status() != AuthStatus.AUTH_OK)
            return Optional.empty();

        var ep = session.endpoint();
        if (ep == null || !ep.isOpen())
            return Optional.empty();

        return Optional.of(session);
    }
}
