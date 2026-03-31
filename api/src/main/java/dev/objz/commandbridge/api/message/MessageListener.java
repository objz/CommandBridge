package dev.objz.commandbridge.api.message;

import dev.objz.commandbridge.api.channel.ChannelPayload;

/**
 * Callback for handling messages received on a typed
 * {@link dev.objz.commandbridge.api.channel.MessageChannel}.
 *
 * <p>This is a {@code @FunctionalInterface} and may be used as a lambda expression.
 * Register an instance via
 * {@link dev.objz.commandbridge.api.channel.MessageChannel#listen(MessageListener)},
 * which returns a {@link dev.objz.commandbridge.api.message.Subscription} that can be
 * used to cancel the listener.
 *
 * <pre>{@code
 * MessageChannel<CommandPayload> channel = api.channel(CommandPayload.class);
 *
 * Subscription sub = channel.listen((ctx, payload) -> {
 *     String from = ctx.from().id();
 *     String command = payload.command();
 *     // handle the incoming command
 * });
 *
 * // To stop receiving messages:
 * sub.cancel();
 * }</pre>
 *
 * @param <T> the type of payload this listener handles;
 *     must extend {@link dev.objz.commandbridge.api.channel.ChannelPayload}
 * @see dev.objz.commandbridge.api.channel.MessageChannel#listen(MessageListener)
 * @see dev.objz.commandbridge.api.message.MessageContext
 * @see dev.objz.commandbridge.api.message.Subscription
 */
@FunctionalInterface
public interface MessageListener<T extends ChannelPayload> {
    /**
     * Invoked when a message matching this listener's payload type is received.
     *
     * @param ctx the metadata for the received message, including the sending server's identity
     *     ({@link dev.objz.commandbridge.api.message.MessageContext#from()}) and the send
     *     timestamp ({@link dev.objz.commandbridge.api.message.MessageContext#timestamp()})
     * @param payload the deserialized message payload
     */
    void accept(MessageContext<T> ctx, T payload);
}
