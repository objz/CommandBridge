package dev.objz.commandbridge.velocity.net.session;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.security.AuthStatus;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class SessionHub implements Iterable<ClientSession> {

    private final ConcurrentHashMap<String, ClientSession> clientsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Endpoint, ClientSession> clientsByEndpoint = new ConcurrentHashMap<>();
    private volatile Consumer<ClientSession> onRemoveListener;

    public void onRemove(Consumer<ClientSession> listener) {
        this.onRemoveListener = listener;
    }

    public ClientSession add(String clientId, Endpoint endpoint) {
        Objects.requireNonNull(endpoint);

        if (clientId == null || clientId.isBlank())
            clientId = "unknown-" + UUID.randomUUID();

        var s = new ClientSession(endpoint, clientId);
        var previous = clientsById.put(clientId, s);
        if (previous != null) {
            if (previous.endpoint() != null) {
                clientsByEndpoint.remove(previous.endpoint());
            }
            notifyRemoval(previous);
        }
        clientsByEndpoint.put(endpoint, s);
        Log.debug("Session created for client '{}' via {}", clientId, endpoint.describe());
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
        if (removed != null) {
            if (removed.endpoint() != null) {
                clientsByEndpoint.remove(removed.endpoint());
            }
            notifyRemoval(removed);
        }
        return removed;
    }

    public ClientSession remove(Endpoint endpoint) {
        if (endpoint == null)
            return null;

        var removed = clientsByEndpoint.remove(endpoint);
        if (removed != null) {
            Log.debug("Session removed for client '{}' via endpoint disconnect", removed.id());
            if (removed.id() != null) {
                clientsById.remove(removed.id(), removed);
            }
            notifyRemoval(removed);
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

    private void notifyRemoval(ClientSession session) {
        var listener = onRemoveListener;
        if (listener != null) {
            try {
                listener.accept(session);
            } catch (Exception e) {
                Log.warn("Session removal listener failed for '{}': {}", session.id(), e.getMessage());
            }
        }
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
