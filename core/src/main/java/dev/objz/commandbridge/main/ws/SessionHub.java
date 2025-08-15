package dev.objz.commandbridge.main.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.main.config.model.VelocityConfig;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.*;
import java.util.concurrent.*;

public final class SessionHub {
	private final Map<WebSocketChannel, ClientSession> byCh = new ConcurrentHashMap<>();
	private final Map<String, ClientSession> byId = new ConcurrentHashMap<>();
	private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
	private final VelocityConfig cfg;
	private final ObjectMapper mapper;

	public SessionHub(VelocityConfig cfg, ObjectMapper mapper) {
		this.cfg = cfg;
		this.mapper = mapper;
	}

	public void start() {
		exec.scheduleAtFixedRate(this::tick, cfg.heartbeat().appPingSeconds(),
				cfg.heartbeat().appPingSeconds(), TimeUnit.SECONDS);
	}

	public void stop() {
		exec.shutdownNow();
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
