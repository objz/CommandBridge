package dev.objz.commandbridge.api.channel;

import dev.objz.commandbridge.api.message.MessageListener;
import dev.objz.commandbridge.api.message.Subscription;
import dev.objz.commandbridge.api.platform.Platform;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * A typed communication pipe for sending and receiving messages across the CommandBridge network.
 *
 * <p>A {@code MessageChannel} is bound to a specific payload type {@code P}, providing type-safe
 * message routing. Obtain an instance via
 * {@link dev.objz.commandbridge.api.CommandBridgeAPI#channel(Class)}.
 * Target one or more servers with {@link #to(java.util.Collection)} or broadcast to all connected
 * servers with {@link #toAll()}. Subscribe to incoming messages with
 * {@link #listen(dev.objz.commandbridge.api.message.MessageListener)}.
 *
 * <p>All send operations return {@link java.util.concurrent.CompletableFuture} and are
 * non-blocking.
 *
 * <pre>{@code
 * CommandBridgeAPI api = CommandBridgeProvider.get();
 * MessageChannel<CommandPayload> channel = api.channel(CommandPayload.class);
 *
 * // Send a command to a specific backend
 * channel.to(List.of(Platform.backend("survival-1")))
 *        .send(new CommandPayload("say hello", RunAs.CONSOLE));
 *
 * // Broadcast to all connected servers
 * channel.toAll().send(new CommandPayload("say maintenance soon", RunAs.CONSOLE));
 *
 * // Listen for incoming commands
 * Subscription sub = channel.listen((ctx, payload) -> {
 *     System.out.println("Command from " + ctx.from().id() + ": " + payload.command());
 * });
 * sub.cancel(); // when done
 * }</pre>
 *
 * @param <P> the type of payload handled by this channel; must extend
 *            {@link dev.objz.commandbridge.api.channel.ChannelPayload}
 * @see dev.objz.commandbridge.api.CommandBridgeAPI#channel(Class)
 * @see Sender
 * @see dev.objz.commandbridge.api.message.MessageListener
 */
public interface MessageChannel<P extends ChannelPayload> {

    /**
     * Returns a {@link Sender} targeting the specified servers.
     *
     * @param targets the destination servers; must not be {@code null} or empty.
     *                The collection is captured at call time.
     * @return a {@code Sender} scoped to the given targets
     * @see #toAll()
     */
    Sender<P> to(Collection<Platform.ServerTarget> targets);

    /**
     * Returns a {@link Sender} that broadcasts to all servers currently connected to the bridge
     * network.
     *
     * @return a {@code Sender} targeting every currently connected server. The set of servers is
     *         determined at send time, not at the time this method is called.
     * @see #to(java.util.Collection)
     */
    Sender<P> toAll();

    /**
     * Subscribes to messages received on this channel.
     *
     * @param listener the callback to invoke for each incoming message; must not be {@code null}
     * @return a {@link dev.objz.commandbridge.api.message.Subscription} that can be cancelled via
     *         {@link dev.objz.commandbridge.api.message.Subscription#cancel()} to stop receiving
     *         messages. The listener remains active until cancelled.
     * @see dev.objz.commandbridge.api.message.Subscription#cancel()
     */
    Subscription listen(MessageListener<P> listener);

    /**
     * A target-bound sender created by {@link MessageChannel#to(java.util.Collection)} or
     * {@link MessageChannel#toAll()}, used to dispatch payloads to the targeted servers.
     *
     * <p>Obtain a {@code Sender} by calling one of the {@link MessageChannel} target methods.
     * Send a payload with {@link #send(ChannelPayload)} for fire-and-forget delivery, or use
     * {@link #request(ChannelPayload)} for request-response patterns. Optional delivery conditions
     * can be set with {@link #requirePlayer(UUID)} and {@link #whenOnline(UUID)}.
     *
     * @param <P> the payload type this sender dispatches; must extend
     *            {@link dev.objz.commandbridge.api.channel.ChannelPayload}
     */
    interface Sender<P extends ChannelPayload> {

        /**
         * Sends a payload to the targeted servers without waiting for a response.
         *
         * @param payload the payload to send; must not be {@code null}
         * @return a {@link java.util.concurrent.CompletableFuture}{@code <Void>} that completes
         *         when the payload has been dispatched (not when it has been received or processed
         *         by the remote server)
         */
        CompletableFuture<Void> send(P payload);

        /**
         * Sends a payload and returns a future that completes with the remote server's response,
         * using the default timeout.
         *
         * <p>This method is only supported for single-target senders. Invoking it on a
         * multi-target sender throws {@link java.lang.UnsupportedOperationException}.
         *
         * @param payload the request payload; must not be {@code null}
         * @return a {@link java.util.concurrent.CompletableFuture} that completes with the
         *         response payload, or completes exceptionally if the default timeout elapses
         *         before a response is received
         * @see #request(ChannelPayload, java.time.Duration)
         */
        CompletableFuture<P> request(P payload);

        /**
         * Sends a payload and returns a future that completes with the remote server's response,
         * using a custom timeout.
         *
         * <p>This method is only supported for single-target senders. Invoking it on a
         * multi-target sender throws {@link java.lang.UnsupportedOperationException}.
         *
         * @param payload the request payload; must not be {@code null}
         * @param timeout the maximum duration to wait for a response; must not be {@code null}
         * @return a {@link java.util.concurrent.CompletableFuture} that completes with the
         *         response payload, or completes exceptionally if the timeout elapses
         * @see #request(ChannelPayload)
         */
        CompletableFuture<P> request(P payload, Duration timeout);

        /**
         * Constrains this sender to only execute if the specified player is present on the target
         * server at the time of delivery.
         *
         * <p>If the player is not present, the command is not delivered to that server. Use
         * {@link #whenOnline(UUID)} instead if queued delivery when the player connects is
         * desired.
         *
         * @param player the UUID of the player whose presence is required; must not be {@code null}
         * @return this sender for method chaining
         * @throws UnsupportedOperationException if this sender implementation does not support
         *                                       player-conditioned delivery (the default
         *                                       implementation always throws)
         * @see #requirePlayer()
         * @see #whenOnline(UUID)
         */
        default Sender<P> requirePlayer(UUID player) {
            throw new UnsupportedOperationException("requirePlayer is not supported by this sender");
        }

        /**
         * Constrains this sender to only execute if the triggering player (if any) is present on
         * the target server at the time of delivery.
         *
         * <p>This is the no-argument variant of {@link #requirePlayer(UUID)}; it uses the player
         * associated with the execution context rather than an explicit UUID.
         *
         * @return this sender for method chaining
         * @throws UnsupportedOperationException if this sender implementation does not support
         *                                       player-conditioned delivery (the default
         *                                       implementation always throws)
         * @see #requirePlayer(UUID)
         */
        default Sender<P> requirePlayer() {
            throw new UnsupportedOperationException("requirePlayer is not supported by this sender");
        }

        /**
         * Queues the payload for delivery to the target server when the specified player is online.
         *
         * <p>Unlike {@link #requirePlayer(UUID)}, which skips delivery if the player is absent,
         * this method queues the payload and delivers it once the player connects to the target
         * server.
         *
         * @param player the UUID of the player to wait for; must not be {@code null}
         * @return this sender for method chaining
         * @throws UnsupportedOperationException if this sender implementation does not support
         *                                       queued delivery (the default implementation always
         *                                       throws)
         * @see #whenOnline()
         * @see #requirePlayer(UUID)
         */
        default Sender<P> whenOnline(UUID player) {
            throw new UnsupportedOperationException("whenOnline is not supported by this sender");
        }

        /**
         * Queues the payload for delivery to the target server when the triggering player (if any)
         * is online.
         *
         * <p>This is the no-argument variant of {@link #whenOnline(UUID)}; it uses the player
         * associated with the execution context rather than an explicit UUID.
         *
         * @return this sender for method chaining
         * @throws UnsupportedOperationException if this sender implementation does not support
         *                                       queued delivery (the default implementation always
         *                                       throws)
         * @see #whenOnline(UUID)
         */
        default Sender<P> whenOnline() {
            throw new UnsupportedOperationException("whenOnline is not supported by this sender");
        }
    }
}
