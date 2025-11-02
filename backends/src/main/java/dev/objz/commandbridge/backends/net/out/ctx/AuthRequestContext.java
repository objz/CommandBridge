package dev.objz.commandbridge.backends.net.out.ctx;

import io.undertow.websockets.core.WebSocketChannel;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

import dev.objz.commandbridge.backends.net.ClientStatus;

public final class AuthRequestContext {
	public final WebSocketChannel ch;
	public final Duration timeout;
	public final Consumer<ClientStatus> statusSink;

	public AuthRequestContext(WebSocketChannel ch, Duration timeout, Consumer<ClientStatus> statusSink) {
		this.ch = Objects.requireNonNull(ch);
		this.timeout = Objects.requireNonNull(timeout);
		this.statusSink = Objects.requireNonNull(statusSink);
	}
}
