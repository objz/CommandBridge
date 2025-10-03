package dev.objz.commandbridge.velocity.ws;

import dev.objz.commandbridge.security.AuthStatus;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public final class ClientSession {
	private final WebSocketChannel ch;
	private volatile String clientId = "unknown";
	private volatile Set<String> caps = Set.of();
	private volatile AuthStatus status = AuthStatus.NOT_AUTHENTICATED;
	private final AtomicLong lastPongNanos = new AtomicLong(System.nanoTime());

	public ClientSession(WebSocketChannel ch) {
		this.ch = ch;
	}

	public WebSocketChannel ch() {
		return ch;
	}

	public AuthStatus status() {
		return status;
	}

	public void markAuthed(String clientId, Set<String> caps) {
		this.clientId = clientId;
		this.caps = (caps != null) ? caps : Set.of();
		this.status = AuthStatus.AUTHENTICATED;
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
