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
 * Router for outbound messages.
 * Handlers extend OutboundHandler to get access to send() functionality.
 * 
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

	/**
	 * Sets the factory function that creates SendOperations for envelopes.
	 * This should be set by WsClient or WsServer during initialization.
	 */
	public OutNode<T> setSendOperationFactory(Function<Envelope, SendOperation> factory) {
		this.sendOperationFactory = factory;
		return this;
	}

	/**
	 * Sets the factory function that creates SendOperations for envelopes with specific channels.
	 * This should be set by WsServer for multi-channel scenarios.
	 */
	public OutNode<T> setChannelSendOperationFactory(BiFunction<WebSocketChannel, Envelope, SendOperation> factory) {
		this.channelSendOperationFactory = factory;
		return this;
	}

	/**
	 * Sets the client ID that will be injected into handlers.
	 */
	public OutNode<T> setClientId(String clientId) {
		this.clientId = clientId;
		return this;
	}

	/**
	 * Sets the server ID that will be injected into handlers.
	 */
	public OutNode<T> setServerId(String serverId) {
		this.serverId = serverId;
		return this;
	}

	/**
	 * Registers a handler for a specific message type.
	 */
	public <C extends T> OutNode<T> register(MessageType type, OutboundHandler<C> handler) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(handler);
		handler.setSendOperationFactory(sendOperationFactory);
		handler.setChannelSendOperationFactory(channelSendOperationFactory);
		handler.setClientId(clientId);
		handler.setServerId(serverId);
		handlers.put(type, handler);
		return this;
	}

	/**
	 * Sends a message of the given type with the provided context.
	 * Returns a SendOperation for chaining .dispatch(), .await(), .match(), .timeout(), etc.
	 * 
	 * @param type The message type to send
	 * @param context The context containing data for building the message
	 * @return SendOperation for chaining
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
			throw new IllegalStateException("SendOperation factory not configured. Call setSendOperationFactory() first.");
		}

		try {
			return ((OutboundHandler<T>) handler).accept(context);
		} catch (Exception ex) {
			Log.error(ex, "Outbound handler threw for {}", type);
			throw new RuntimeException("Failed to send message for " + type, ex);
		}
	}

	/**
	 * Sends a message of the given type without additional context.
	 * For use when the handler doesn't need context.
	 * 
	 * @param type The message type to send
	 * @return SendOperation for chaining
	 */
	public SendOperation send(MessageType type) {
		return send(type, null);
	}
}
