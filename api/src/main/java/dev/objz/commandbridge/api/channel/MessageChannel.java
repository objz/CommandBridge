package dev.objz.commandbridge.api.channel;

import dev.objz.commandbridge.api.message.MessageListener;
import dev.objz.commandbridge.api.message.Subscription;
import dev.objz.commandbridge.api.platform.Platform;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Communication pipe for sending and receiving {@link ChannelPayload}s.
 *
 * @param <P> the type of payload handled by this channel
 */
public interface MessageChannel<P extends ChannelPayload> {

    /**
     * Targets one or more servers for sending.
     *
     * @param targets the destination servers
     * @return a sender scoped to the given targets
     */
    Sender<P> to(Collection<Platform.ServerTarget> targets);

    /**
     * Targets all connected servers for sending.
     *
     * @return a sender that broadcasts to every connected server
     */
    Sender<P> toAll();

    /**
     * Subscribes a listener to messages received on this channel.
     *
     * @param listener the listener to call for incoming messages
     * @return a subscription handle to cancel the listener
     */
    Subscription listen(MessageListener<P> listener);

    /**
     * A target-bound sender for dispatching payloads.
     *
     * @param <P> the payload type
     */
    interface Sender<P extends ChannelPayload> {

        /**
         * Sends a payload without expecting a response.
         *
         * @param payload the data to send
         * @return a future that completes when the message is dispatched
         */
        CompletableFuture<Void> send(P payload);

        /**
         * Sends a request and waits for a response with the default timeout.
         * Only supported for single-target senders.
         *
         * @param payload the request data
         * @return a future containing the response payload
         */
        CompletableFuture<P> request(P payload);

        /**
         * Sends a request and waits for a response with a custom timeout.
         * Only supported for single-target senders.
         *
         * @param payload the request data
         * @param timeout the maximum time to wait for a response
         * @return a future containing the response payload
         */
        CompletableFuture<P> request(P payload, Duration timeout);

        default Sender<P> requirePlayer(UUID player) {
            throw new UnsupportedOperationException("requirePlayer is not supported by this sender");
        }

        default Sender<P> requirePlayer() {
            throw new UnsupportedOperationException("requirePlayer is not supported by this sender");
        }

        default Sender<P> whenOnline(UUID player) {
            throw new UnsupportedOperationException("whenOnline is not supported by this sender");
        }

        default Sender<P> whenOnline() {
            throw new UnsupportedOperationException("whenOnline is not supported by this sender");
        }
    }
}
