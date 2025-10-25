package dev.objz.commandbridge.velocity.net.route;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.proto.Envelope;
import dev.objz.commandbridge.proto.MessageType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class OutboundRouter {

	@FunctionalInterface
	public interface OutboundHandler<A> {
		CompletableFuture<Envelope> accept(A args);
	}

	public interface Typed<A> {
		Class<A> argType();
	}

	private final Map<MessageType, OutboundHandler<?>> handlers;

	public OutboundRouter() {
		this.handlers = new EnumMap<>(MessageType.class);
	}

	public <A> OutboundRouter register(MessageType type, OutboundHandler<A> handler) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(handler);
		handlers.put(type, handler);
		return this;
	}

	public OutboundRouter unregister(MessageType type) {
		Objects.requireNonNull(type);
		handlers.remove(type);
		return this;
	}

	public CompletableFuture<Envelope> send(MessageType type) {
		return sendInternal(type, null);
	}

	public <A> CompletableFuture<Envelope> send(MessageType type, A args) {
		return sendInternal(type, args);
	}

	// I don't like this side of java either
	// but idk how it will be possible with type safety otherwise
	private <A> CompletableFuture<Envelope> sendInternal(MessageType type, A args) {
		if (type == null) {
			var cf = new CompletableFuture<Envelope>();
			cf.completeExceptionally(new IllegalArgumentException("send() requires a MessageType"));
			return cf;
		}

		var h = handlers.get(type);
		if (h == null) {
			Log.warn("No OutboundHandler registered for: {}", type);
			var cf = new CompletableFuture<Envelope>();
			cf.completeExceptionally(
					new IllegalStateException("No OutboundHandler registered for " + type));
			return cf;
		}

		try {
			if (h instanceof Typed<?> t) {
				Class<?> expected = t.argType();
				if (args != null && !expected.isInstance(args)) {
					var cf = new CompletableFuture<Envelope>();
					cf.completeExceptionally(new IllegalArgumentException(
							"Argument type mismatch for " + type + ": expected "
									+ expected.getName() +
									", got " + args.getClass().getName()));
					return cf;
				}
			}

			return ((OutboundHandler<A>) h).accept(args);
		} catch (Exception ex) {
			Log.error(ex, "Outbound handler threw for {}", type);
			var cf = new CompletableFuture<Envelope>();
			cf.completeExceptionally(ex);
			return cf;
		}
	}
}
