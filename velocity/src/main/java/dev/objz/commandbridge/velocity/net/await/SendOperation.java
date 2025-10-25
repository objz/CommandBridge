package dev.objz.commandbridge.velocity.net.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.proto.Envelope;
import dev.objz.commandbridge.proto.MessageType;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public final class SendOperation {

	private final WebSocketChannel ch;
	private final Envelope request;
	private final ResponseAwaiter awaiter;
	private final ObjectMapper mapper;

	private MessageType expected;
	private Predicate<Envelope> matcher;
	private Duration timeout = Duration.ofSeconds(15);

	public SendOperation(WebSocketChannel ch,
			Envelope request,
			ResponseAwaiter awaiter,
			ObjectMapper mapper) {
		this.ch = Objects.requireNonNull(ch);
		this.request = Objects.requireNonNull(request);
		this.awaiter = Objects.requireNonNull(awaiter);
		this.mapper = Objects.requireNonNull(mapper);
	}

	public SendOperation expect(MessageType type) {
		this.expected = type;
		return this;
	}

	public SendOperation match(Predicate<Envelope> matcher) {
		this.matcher = Objects.requireNonNull(matcher);
		return this;
	}

	public SendOperation timeout(Duration timeout) {
		this.timeout = Objects.requireNonNull(timeout);
		return this;
	}

	public CompletableFuture<Envelope> await() {
		Predicate<Envelope> m = (matcher != null)
				? matcher
				: (env -> Objects.equals(request.id(), env.id()) &&
						(expected == null || expected == env.type()));

		var clientId = request.to();
		var fut = awaiter.await(clientId, String.valueOf(request.id()), m, timeout);
		try {
			WebSockets.sendText(mapper.writeValueAsString(request), ch, null);
		} catch (Exception e) {
			fut.completeExceptionally(e);
		}
		return fut;
	}

	public CompletableFuture<Void> dispatch() {
		try {
			WebSockets.sendText(mapper.writeValueAsString(request), ch, null);
			return CompletableFuture.completedFuture(null);
		} catch (Exception e) {
			var cf = new CompletableFuture<Void>();
			cf.completeExceptionally(e);
			return cf;
		}
	}
}
