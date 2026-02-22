package dev.objz.commandbridge.velocity.net.session;

import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.security.AuthStatus;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class SessionHub implements Iterable<ClientSession> {

    private final ConcurrentHashMap<WebSocketChannel, ClientSession> clients = new ConcurrentHashMap<>();

    public ClientSession add(WebSocketChannel ch, String clientId) {
        Objects.requireNonNull(ch);
        if (clientId == null || clientId.isBlank())
            clientId = "unknown";
        var s = new ClientSession(ch, clientId);
        s.status(AuthStatus.AUTH_OK);
        clients.put(ch, s);
        ch.getCloseSetter().set(c -> remove((WebSocketChannel) c));
        return s;
    }

    public ClientSession get(WebSocketChannel ch) {
        return clients.get(ch);
    }

    public boolean contains(WebSocketChannel ch) {
        return clients.containsKey(ch);
    }

    public ClientSession remove(WebSocketChannel ch) {
        if (ch == null)
            return null;
        return clients.remove(ch);
    }

    public void clear() {
        clients.clear();
    }

    public int size() {
        return clients.size();
    }

    @Override
    public Iterator<ClientSession> iterator() {
        return clients.values().iterator();
    }

    public <T> ClientSession set(WebSocketChannel ch, BiConsumer<ClientSession, T> setter, T value) {
        Objects.requireNonNull(setter);
        return clients.computeIfPresent(ch, (k, s) -> {
            setter.accept(s, value);
            return s;
        });
    }

    public Optional<ClientSession> findSession(String id, Location location) {
        if (id == null || location == null)
            return Optional.empty();

        for (ClientSession session : clients.values()) {
            if (id.equals(session.id())
                    && session.location() == location
                    && session.status() == AuthStatus.AUTH_OK
                    && session.ch() != null
                    && session.ch().isOpen()) {
                return Optional.of(session);
            }
        }
        return Optional.empty();
    }
}
