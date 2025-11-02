package dev.objz.commandbridge.net;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.proto.MessageType;

import io.undertow.websockets.core.WebSocketChannel;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import dev.objz.commandbridge.net.proto.Envelope;

/**
 * @param <T> The context type that handlers receive
 */
public class OutNode<T> {

	private final Map<MessageType, OutboundHandler<? super T>> handlers;
	private Function<Envelope, SendOperation> sendOperationFactory;
	private BiFunction<WebSocketChannel, Envelope, SendOperation> channelSendOperationFactory;
	private String clientId;
	private String serverId;

	public OutNode() {
		this.handlers = new EnumMap<>(MessageType.class);
	}

	public OutNode<T> setSendOperationFactory(Function<Envelope, SendOperation> factory) {
		this.sendOperationFactory = factory;
		return this;
	}

	public OutNode<T> setChannelSendOperationFactory(
			BiFunction<WebSocketChannel, Envelope, SendOperation> factory) {
		this.channelSendOperationFactory = factory;
		return this;
	}

	public OutNode<T> setClientId(String clientId) {
		this.clientId = clientId;
		return this;
	}

	public OutNode<T> setServerId(String serverId) {
		this.serverId = serverId;
		return this;
	}

	public <C extends T> OutNode<T> register(MessageType type, OutboundHandler<C> handler) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(handler);
		handler.setSendOperationFactory(sendOperationFactory);
		handler.setChannelSendOperationFactory(channelSendOperationFactory);
		handler.setClientId(clientId);
		handler.setServerId(serverId);
		@SuppressWarnings("unchecked")
		OutboundHandler<? super T> typedHandler = (OutboundHandler<? super T>) handler;
		handlers.put(type, typedHandler);
		return this;
	}

	/**
	 * @param type    The message type to send
	 * @param context The context containing data for building the message
	 * @return SendOperation
	 */
	public SendOperation send(MessageType type, T context) {
		if (type == null) {
			throw new IllegalArgumentException("MessageType cannot be null");
		}

		var handler = handlers.get(type);
		if (handler == null) {
			Log.warn("No OutboundHandler registered for: {}", type);
			throw new IllegalStateException("No OutboundHandler registered for " + type);
		}

		if (sendOperationFactory == null && channelSendOperationFactory == null) {
			throw new IllegalStateException(
					"SendOperation factory not configured. Call setSendOperationFactory() first.");
		}

		try {
			@SuppressWarnings("unchecked")
			OutboundHandler<T> typedHandler = (OutboundHandler<T>) handler;
			return typedHandler.accept(context);
		} catch (Exception ex) {
			Log.error(ex, "Outbound handler threw for {}", type);
			throw new RuntimeException("Failed to send message for " + type, ex);
		}
	}

	/**
	 * @param type The message type to send
	 * @return SendOperation
	 */
	public SendOperation send(MessageType type) {
		return send(type, null);
	}
}
