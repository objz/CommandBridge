package dev.objz.commandbridge.velocity.ws;

import dev.objz.commandbridge.main.config.model.VelocityConfig;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.MessageType;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class SessionHub {
	private final Map<WebSocketChannel, ClientSession> byCh = new ConcurrentHashMap<>();
	private final Map<String, ClientSession> byId = new ConcurrentHashMap<>();
	private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
	private final VelocityConfig cfg;
	private final ObjectMapper mapper;
	private final FeedbackAwaiter feedback = new FeedbackAwaiter();

	private final List<Consumer<ClientSession>> authedListeners = new CopyOnWriteArrayList<>();

	public SessionHub(VelocityConfig cfg, ObjectMapper mapper) {
		this.cfg = cfg;
		this.mapper = mapper;
	}

	public void expectFeedback(String envelopeId, MessageType resultType, java.time.Duration timeout,
			String opName, String backendId) {
		feedback.expect(envelopeId, resultType, timeout, opName, backendId);
	}

	public void completeFeedback(Envelope env) {
		feedback.complete(env);
	}

	// callback
	public void onAuthed(Consumer<ClientSession> listener) {
		authedListeners.add(listener);
	}

	public void start() {
		exec.scheduleAtFixedRate(this::tick, cfg.heartbeat().appPingSeconds(),
				cfg.heartbeat().appPingSeconds(), TimeUnit.SECONDS);
	}

	public void stop() {
		exec.shutdownNow();
		feedback.shutdown();
	}

	public ClientSession register(WebSocketChannel ch) {
		var s = new ClientSession(ch);
		byCh.put(ch, s);
		Log.info("Client connected: {}", ch.getSourceAddress());
		return s;
	}

	public void authed(WebSocketChannel ch, String clientId, Set<String> caps) {
		var s = byCh.get(ch);
		if (s == null)
			return;
		s.markAuthed(clientId, caps);
		byId.put(clientId, s);
		Log.success("Authenticated client '{}'", clientId);

		for (Consumer<ClientSession> c : authedListeners) {
			try {
				c.accept(s);
			} catch (Exception e) {
				Log.warn("onAuthed listener failed: {}", e.toString());
			}
		}
	}

	public void remove(WebSocketChannel ch) {
		var s = byCh.remove(ch);
		if (s != null) {
			byId.remove(s.clientId(), s);
			Log.info("Client disconnected: {} ({})", s.clientId(), ch.getSourceAddress());
		}
	}

	public ClientSession byClientId(String id) {
		return byId.get(id);
	}

	public Collection<ClientSession> all() {
		return byCh.values();
	}

	public void send(WebSocketChannel ch, Envelope env) {
		try {
			WebSockets.sendText(mapper.writeValueAsString(env), ch, null);
		} catch (Exception e) {
			Log.error(e, "Send failed to {}", ch.getSourceAddress());
		}
	}

	private void tick() {
		long staleNs = TimeUnit.SECONDS.toNanos(cfg.heartbeat().staleAfterSeconds());
		long now = System.nanoTime();
		for (var s : all()) {
			if (!s.authed())
				continue;
			if (now - s.lastPongNanos() > staleNs) {
				Log.warn("Client '{}' stale (no PONG). Closing", s.clientId());
				try {
					s.ch().close();
				} catch (Exception ignored) {
				}
				remove(s.ch());
			} else {
				var ping = Envelope.ping(cfg.serverId());
				send(s.ch(), ping);
			}
		}
	}
}
