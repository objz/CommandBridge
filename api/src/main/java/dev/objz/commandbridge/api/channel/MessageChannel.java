package dev.objz.commandbridge.api.channel;

import dev.objz.commandbridge.api.message.MessageListener;
import dev.objz.commandbridge.api.message.Subscription;
import dev.objz.commandbridge.api.platform.Platform;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Communication pipe for sending and receiving {@link ChannelPayload}s.
 *
 * @param <P> the type of payload handled by this channel
 */
public interface MessageChannel<P extends ChannelPayload> {

    /**
     * Sends a payload to a target server without expecting a response.
     *
     * @param target the destination server
     * @param payload the data to send
     * @return a future that completes when the message is sent
     */
    CompletableFuture<Void> send(Platform.ServerTarget target, P payload);

    /**
     * Sends a request to a target server and waits for a response.
     *
     * @param target the destination server
     * @param payload the request data
     * @return a future containing the response payload
     */
    CompletableFuture<P> request(Platform.ServerTarget target, P payload);

    /**
     * Sends a request to a target server with a custom timeout.
     *
     * @param target the destination server
     * @param payload the request data
     * @param timeout the maximum time to wait for a response
     * @return a future containing the response payload
     */
    CompletableFuture<P> request(Platform.ServerTarget target, P payload, Duration timeout);

    /**
     * Subscribes a listener to messages received on this channel.
     *
     * @param listener the listener to call for incoming messages
     * @return a subscription handle to cancel the listener
     */
    Subscription listen(MessageListener<P> listener);
}
