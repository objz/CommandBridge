package dev.objz.commandbridge.api.channel;

import dev.objz.commandbridge.api.message.MessageListener;
import dev.objz.commandbridge.api.message.Subscription;
import dev.objz.commandbridge.api.platform.Platform;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public interface MessageChannel<P extends ChannelPayload> {

    CompletableFuture<Void> send(Platform.ServerTarget target, P payload);

    CompletableFuture<P> request(Platform.ServerTarget target, P payload);

    CompletableFuture<P> request(Platform.ServerTarget target, P payload, Duration timeout);

    Subscription listen(MessageListener<P> listener);
}
