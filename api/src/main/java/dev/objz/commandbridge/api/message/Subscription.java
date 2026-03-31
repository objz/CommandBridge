package dev.objz.commandbridge.api.message;

/**
 * A handle to a registered listener that allows the registration to be cancelled.
 *
 * <p>When registering a listener via
 * {@link dev.objz.commandbridge.api.channel.MessageChannel#listen(dev.objz.commandbridge.api.message.MessageListener)}
 * or {@link dev.objz.commandbridge.api.CommandBridgeAPI#onServerConnected(dev.objz.commandbridge.api.message.ServerEventListener)},
 * a {@code Subscription} is returned. The caller is responsible for storing it and calling
 * {@link #cancel()} when the listener is no longer needed, for example during plugin shutdown.
 *
 * <p>Calling {@link #cancel()} is idempotent: invoking it more than once has no effect.
 *
 * <p>To register a listener on a channel and retain the subscription:
 *
 * <pre>{@code
 * Subscription sub = api.channel(CommandPayload.class)
 *     .listen((ctx, payload) -> handleCommand(ctx, payload));
 * }</pre>
 *
 * <p>To cancel the listener, for example during plugin shutdown:
 *
 * <pre>{@code
 * sub.cancel();
 * }</pre>
 *
 * @see dev.objz.commandbridge.api.channel.MessageChannel#listen(dev.objz.commandbridge.api.message.MessageListener)
 * @see dev.objz.commandbridge.api.CommandBridgeAPI#onServerConnected(dev.objz.commandbridge.api.message.ServerEventListener)
 * @see dev.objz.commandbridge.api.message.MessageListener
 */
@FunctionalInterface
public interface Subscription {

    /**
     * Unregisters the associated listener so it receives no further events.
     *
     * <p>This method is idempotent; subsequent calls after the first have no effect.
     */
    void cancel();
}
