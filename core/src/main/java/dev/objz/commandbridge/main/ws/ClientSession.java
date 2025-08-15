package dev.objz.commandbridge.main.ws;

import io.undertow.websockets.core.WebSocketChannel;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class ClientSession {
	private final WebSocketChannel ch;
	private volatile String clientId = "unknown";
	private volatile Set<String> caps = Set.of();
	private final AtomicBoolean authed = new AtomicBoolean(false);
	private final AtomicLong lastPongNanos = new AtomicLong(System.nanoTime());

	public ClientSession(WebSocketChannel ch) {
		this.ch = ch;
	}

	public WebSocketChannel ch() {
		return ch;
	}

	public boolean authed() {
		return authed.get();
	}

	public void markAuthed(String clientId, Set<String> caps) {
		this.clientId = clientId;
		this.caps = caps;
		this.authed.set(true);
	}

	public String clientId() {
		return clientId;
	}

	public Set<String> caps() {
		return caps;
	}

	public void touchPong() {
		lastPongNanos.set(System.nanoTime());
	}

	public long lastPongNanos() {
		return lastPongNanos.get();
	}
}
