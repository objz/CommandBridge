package dev.objz.commandbridge.net;

import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import io.undertow.websockets.core.WebSocketChannel;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Base class for outbound message handlers.
 * Provides send() method that creates and sends messages with SendOperation chaining.
 * 
 * @param <T> The context type that contains data for building the outbound message
 */
public abstract class OutboundHandler<T> {
	
	protected String clientId;
	protected String serverId;
	private Function<Envelope, SendOperation> sendOperationFactory;
	private BiFunction<WebSocketChannel, Envelope, SendOperation> channelSendOperationFactory;

	/**
	 * Called by the framework to inject the SendOperation factory (for single channel).
	 */
	public void setSendOperationFactory(Function<Envelope, SendOperation> factory) {
		this.sendOperationFactory = factory;
	}

	/**
	 * Called by the framework to inject the SendOperation factory (for multi-channel scenarios).
	 */
	public void setChannelSendOperationFactory(BiFunction<WebSocketChannel, Envelope, SendOperation> factory) {
		this.channelSendOperationFactory = factory;
	}

	/**
	 * Called by the framework to set client ID.
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * Called by the framework to set server ID.
	 */
	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	/**
	 * Handles an outbound message request.
	 * Implementations should create and send the envelope.
	 * 
	 * @param context The context containing data for building the message
	 * @return SendOperation for chaining .dispatch(), .await(), .match(), etc.
	 */
	public abstract SendOperation accept(T context);

	/**
	 * Creates and sends an envelope (single channel mode).
	 * 
	 * @param envelope The envelope to send
	 * @return SendOperation for chaining .dispatch(), .await(), .match(), etc.
	 */
	protected SendOperation send(Envelope envelope) {
		if (sendOperationFactory == null) {
			throw new IllegalStateException("SendOperation factory not configured");
		}
		return sendOperationFactory.apply(envelope);
	}

	/**
	 * Creates and sends an envelope to a specific channel (multi-channel mode).
	 * 
	 * @param channel The WebSocket channel to send to
	 * @param envelope The envelope to send
	 * @return SendOperation for chaining .dispatch(), .await(), .match(), etc.
	 */
	protected SendOperation send(WebSocketChannel channel, Envelope envelope) {
		if (channelSendOperationFactory == null) {
			throw new IllegalStateException("Channel SendOperation factory not configured");
		}
		return channelSendOperationFactory.apply(channel, envelope);
	}
}

