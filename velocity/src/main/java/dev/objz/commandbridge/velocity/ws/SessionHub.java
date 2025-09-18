package dev.objz.commandbridge.velocity.ws;

import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.security.AuthStatus;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class SessionHub {
	private final Map<WebSocketChannel, ClientSession> byCh = new ConcurrentHashMap<>();
	private final Map<String, ClientSession> byId = new ConcurrentHashMap<>();

	private final List<Consumer<ClientSession>> authedListeners = new CopyOnWriteArrayList<>();

	private record FbWait(String expectedFrom, CompletableFuture<Envelope> fut) {
	}

	private final Map<String, FbWait> feedbackWaiters = new ConcurrentHashMap<>();

	public void start() {
		/* no-op */ }

	public void stop() {
		feedbackWaiters.clear();
		byCh.clear();
		byId.clear();
		authedListeners.clear();
	}

	public void onAuthed(Consumer<ClientSession> listener) {
		authedListeners.add(Objects.requireNonNull(listener, "listener"));
	}

	public void register(WebSocketChannel ch) {
		byCh.put(ch, new ClientSession(ch));
	}

	public void remove(WebSocketChannel ch) {
		ClientSession s = byCh.remove(ch);
		if (s != null && s.clientId() != null)
			byId.remove(s.clientId());
	}

	public ClientSession find(WebSocketChannel ch) {
		return byCh.get(ch);
	}

	public Collection<ClientSession> all() {
		return byCh.values();
	}

	public void authed(WebSocketChannel ch, String clientId, Set<String> caps) {
		ClientSession s = byCh.get(ch);
		if (s == null)
			return;

		s.markAuthed(clientId, (caps != null ? caps : Set.of()));
		byId.put(clientId, s);
		Log.success(true, "Authenticated client '{}'", clientId);

		for (var l : authedListeners) {
			try {
				l.accept(s);
			} catch (Throwable t) {
				Log.warn("onAuthed listener failed: {}", t.toString());
			}
		}
	}

	public void send(WebSocketChannel ch, Envelope env) {
		ClientSession s = byCh.get(ch);
		boolean authed = (s != null && s.status() == AuthStatus.AUTHENTICATED);
		if (!dev.objz.commandbridge.main.proto.PreAuth.proxyOutboundAllowed(authed, env.type())) {
			Log.warn("Block send {} to unauthenticated {}", env.type(), ch.getSourceAddress());
			return;
		}
		WsIO.sendText(ch, env);
	}

	public void expectFeedback(String id, String expectedFrom, CompletableFuture<Envelope> fut) {
		feedbackWaiters.put(id, new FbWait(expectedFrom, fut));
	}

	public void completeFeedback(Envelope env) {
		FbWait w = feedbackWaiters.remove(String.valueOf(env.id()));
		if (w == null)
			return;
		if (w.expectedFrom != null && !w.expectedFrom.equals(env.from())) {
			Log.warn("Ignoring FEEDBACK {}, unexpected sender '{}', expected '{}'",
					env.id(), env.from(), w.expectedFrom);
			return;
		}
		w.fut.complete(env);
	}

	public Optional<ClientSession> byClientId(String clientId) {
		return Optional.ofNullable(byId.get(clientId));
	}
}
