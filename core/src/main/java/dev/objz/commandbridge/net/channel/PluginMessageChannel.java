package dev.objz.commandbridge.net.channel;

import dev.objz.commandbridge.api.channel.ChannelPayload;
import dev.objz.commandbridge.api.channel.MessageChannel;
import dev.objz.commandbridge.api.message.MessageListener;
import dev.objz.commandbridge.api.message.Subscription;
import dev.objz.commandbridge.api.platform.Platform;
import dev.objz.commandbridge.net.payloads.PluginMessage;
import dev.objz.commandbridge.net.proto.Envelope;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class PluginMessageChannel<P extends ChannelPayload> implements MessageChannel<P> {

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

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    private final Class<P> payloadType;
    private final SendTransport sendTransport;
    private final RequestTransport requestTransport;
    private final ListenerRegistrar<P> listenerRegistrar;

    public PluginMessageChannel(Class<P> payloadType,
            SendTransport sendTransport,
            RequestTransport requestTransport,
            ListenerRegistrar<P> listenerRegistrar) {
        this.payloadType = Objects.requireNonNull(payloadType);
        this.sendTransport = Objects.requireNonNull(sendTransport);
        this.requestTransport = Objects.requireNonNull(requestTransport);
        this.listenerRegistrar = Objects.requireNonNull(listenerRegistrar);
    }

    @Override
    public Sender<P> to(Collection<Platform.ServerTarget> targets) {
        Objects.requireNonNull(targets);
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("At least one target is required");
        }
        if (targets.size() == 1) {
            return new SingleTargetSender(Objects.requireNonNull(targets.iterator().next()));
        }
        Set<Platform.ServerTarget> targetSet = new LinkedHashSet<>();
        for (Platform.ServerTarget target : targets) {
            targetSet.add(Objects.requireNonNull(target));
        }
        return new MultiTargetSender(targetSet);
    }

    @Override
    public Sender<P> toAll() {
        return new SingleTargetSender(Platform.BACKEND.target("*"));
    }

    @Override
    public Subscription listen(MessageListener<P> listener) {
        Objects.requireNonNull(listener);
        return listenerRegistrar.listen(listener);
    }

    private PluginMessage toPluginMessage(P payload, boolean expectsResponse) {
        return new PluginMessage(payloadType.getName(),
                Envelope.MAPPER.valueToTree(payload), expectsResponse);
    }

    private P toPayload(PluginMessage response) {
        try {
            return Envelope.MAPPER.treeToValue(response.data(), payloadType);
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    private final class SingleTargetSender implements Sender<P> {

        private final Platform.ServerTarget target;

        SingleTargetSender(Platform.ServerTarget target) {
            this.target = target;
        }

        @Override
        public CompletableFuture<Void> send(P payload) {
            Objects.requireNonNull(payload);
            return sendTransport.send(target, toPluginMessage(payload, false));
        }

        @Override
        public CompletableFuture<P> request(P payload) {
            return request(payload, DEFAULT_TIMEOUT);
        }

        @Override
        public CompletableFuture<P> request(P payload, Duration timeout) {
            Objects.requireNonNull(payload);
            Objects.requireNonNull(timeout);
            return requestTransport.request(target, toPluginMessage(payload, true), timeout)
                    .thenApply(PluginMessageChannel.this::toPayload);
        }
    }

    private final class MultiTargetSender implements Sender<P> {

        private final Set<Platform.ServerTarget> targets;

        MultiTargetSender(Set<Platform.ServerTarget> targets) {
            this.targets = targets;
        }

        @Override
        public CompletableFuture<Void> send(P payload) {
            Objects.requireNonNull(payload);
            PluginMessage message = toPluginMessage(payload, false);
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (Platform.ServerTarget target : targets) {
                futures.add(sendTransport.send(target, message));
            }
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        }

        @Override
        public CompletableFuture<P> request(P payload) {
            throw new UnsupportedOperationException("Request is only supported for single-target senders");
        }

        @Override
        public CompletableFuture<P> request(P payload, Duration timeout) {
            throw new UnsupportedOperationException("Request is only supported for single-target senders");
        }
    }
}
