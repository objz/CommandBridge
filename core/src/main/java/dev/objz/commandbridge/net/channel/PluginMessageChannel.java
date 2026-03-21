package dev.objz.commandbridge.net.channel;

import dev.objz.commandbridge.api.channel.ChannelPayload;
import dev.objz.commandbridge.api.channel.ChannelType;
import dev.objz.commandbridge.api.channel.MessageChannel;
import dev.objz.commandbridge.api.message.MessageListener;
import dev.objz.commandbridge.api.message.Subscription;
import dev.objz.commandbridge.api.platform.Platform;
import dev.objz.commandbridge.net.payloads.PluginMessage;
import dev.objz.commandbridge.net.proto.Envelope;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class PluginMessageChannel<P extends ChannelPayload> implements MessageChannel<P> {

    @FunctionalInterface
    public interface SendTransport {
        CompletableFuture<Void> send(Platform.ServerTarget target, PluginMessage payload);
    }

    @FunctionalInterface
    public interface RequestTransport {
        CompletableFuture<PluginMessage> request(Platform.ServerTarget target, PluginMessage payload, Duration timeout);
    }

    @FunctionalInterface
    public interface ListenerRegistrar<T extends ChannelPayload> {
        Subscription listen(MessageListener<T> listener);
    }

    private final ChannelType<P, ? extends MessageChannel<P>> channelType;
    private final SendTransport sendTransport;
    private final RequestTransport requestTransport;
    private final ListenerRegistrar<P> listenerRegistrar;

    public PluginMessageChannel(ChannelType<P, ? extends MessageChannel<P>> channelType,
            SendTransport sendTransport,
            RequestTransport requestTransport,
            ListenerRegistrar<P> listenerRegistrar) {
        this.channelType = Objects.requireNonNull(channelType);
        this.sendTransport = Objects.requireNonNull(sendTransport);
        this.requestTransport = Objects.requireNonNull(requestTransport);
        this.listenerRegistrar = Objects.requireNonNull(listenerRegistrar);
    }

    @Override
    public CompletableFuture<Void> send(Platform.ServerTarget target, P payload) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(payload);
        return sendTransport.send(target, toPluginMessage(payload, false));
    }

    @Override
    public CompletableFuture<P> request(Platform.ServerTarget target, P payload) {
        return request(target, payload, Duration.ofSeconds(15));
    }

    @Override
    public CompletableFuture<P> request(Platform.ServerTarget target, P payload, Duration timeout) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(payload);
        Objects.requireNonNull(timeout);
        return requestTransport.request(target, toPluginMessage(payload, true), timeout)
                .thenApply(this::toPayload);
    }

    @Override
    public Subscription listen(MessageListener<P> listener) {
        Objects.requireNonNull(listener);
        return listenerRegistrar.listen(listener);
    }

    private PluginMessage toPluginMessage(P payload, boolean expectsResponse) {
        return new PluginMessage(channelType.getClass().getName(),
                Envelope.MAPPER.valueToTree(payload), expectsResponse);
    }

    private P toPayload(PluginMessage response) {
        try {
            return Envelope.MAPPER.treeToValue(response.data(), channelType.type());
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }
}
