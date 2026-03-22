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
import java.util.UUID;
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
        return new BroadcastSender();
    }

    @Override
    public Subscription listen(MessageListener<P> listener) {
        Objects.requireNonNull(listener);
        return listenerRegistrar.listen(listener);
    }

    private PluginMessage toPluginMessage(P payload, boolean expectsResponse, UUID requirePlayer, UUID whenOnline) {
        return new PluginMessage(payloadType.getName(),
                Envelope.MAPPER.valueToTree(payload), expectsResponse, requirePlayer, whenOnline, null);
    }

    private PluginMessage toPluginMessage(P payload, boolean expectsResponse) {
        return toPluginMessage(payload, expectsResponse, null, null);
    }

    private UUID resolveConditionUuid(P payload, UUID stored, boolean fromPayload) {
        if (!fromPayload) {
            return stored;
        }
        if (payload instanceof dev.objz.commandbridge.api.channel.command.CommandPayload cp) {
            UUID uuid = cp.player();
            if (uuid == null) {
                throw new IllegalStateException("Payload has no player UUID");
            }
            return uuid;
        }
        throw new IllegalStateException("No-arg condition requires CommandPayload with a player UUID");
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
        private UUID requirePlayerUuid;
        private boolean requirePlayerFromPayload;
        private UUID whenOnlineUuid;
        private boolean whenOnlineFromPayload;

        SingleTargetSender(Platform.ServerTarget target) {
            this.target = target;
        }

        private boolean hasCondition() {
            return requirePlayerUuid != null || requirePlayerFromPayload || whenOnlineUuid != null || whenOnlineFromPayload;
        }

        @Override
        public CompletableFuture<Void> send(P payload) {
            Objects.requireNonNull(payload);
            UUID requirePlayer = PluginMessageChannel.this
                    .resolveConditionUuid(payload, requirePlayerUuid, requirePlayerFromPayload);
            UUID whenOnline = PluginMessageChannel.this.resolveConditionUuid(payload, whenOnlineUuid, whenOnlineFromPayload);
            return sendTransport.send(target, toPluginMessage(payload, false, requirePlayer, whenOnline));
        }

        @Override
        public CompletableFuture<P> request(P payload) {
            return request(payload, DEFAULT_TIMEOUT);
        }

        @Override
        public CompletableFuture<P> request(P payload, Duration timeout) {
            Objects.requireNonNull(payload);
            Objects.requireNonNull(timeout);
            if (hasCondition() && (whenOnlineUuid != null || whenOnlineFromPayload)) {
                throw new UnsupportedOperationException("whenOnline is not supported for request");
            }
            UUID requirePlayer = PluginMessageChannel.this
                    .resolveConditionUuid(payload, requirePlayerUuid, requirePlayerFromPayload);
            return requestTransport.request(target, toPluginMessage(payload, true, requirePlayer, null), timeout)
                    .thenApply(PluginMessageChannel.this::toPayload);
        }

        @Override
        public Sender<P> requirePlayer(UUID player) {
            Objects.requireNonNull(player);
            if (whenOnlineUuid != null || whenOnlineFromPayload) {
                throw new IllegalStateException("Cannot combine requirePlayer and whenOnline");
            }
            requirePlayerUuid = player;
            requirePlayerFromPayload = false;
            return this;
        }

        @Override
        public Sender<P> requirePlayer() {
            if (whenOnlineUuid != null || whenOnlineFromPayload) {
                throw new IllegalStateException("Cannot combine requirePlayer and whenOnline");
            }
            requirePlayerFromPayload = true;
            requirePlayerUuid = null;
            return this;
        }

        @Override
        public Sender<P> whenOnline(UUID player) {
            Objects.requireNonNull(player);
            if (requirePlayerUuid != null || requirePlayerFromPayload) {
                throw new IllegalStateException("Cannot combine requirePlayer and whenOnline");
            }
            whenOnlineUuid = player;
            whenOnlineFromPayload = false;
            return this;
        }

        @Override
        public Sender<P> whenOnline() {
            if (requirePlayerUuid != null || requirePlayerFromPayload) {
                throw new IllegalStateException("Cannot combine requirePlayer and whenOnline");
            }
            whenOnlineFromPayload = true;
            whenOnlineUuid = null;
            return this;
        }
    }

    private final class MultiTargetSender implements Sender<P> {

        private final Set<Platform.ServerTarget> targets;
        private UUID requirePlayerUuid;
        private boolean requirePlayerFromPayload;
        private UUID whenOnlineUuid;
        private boolean whenOnlineFromPayload;

        MultiTargetSender(Set<Platform.ServerTarget> targets) {
            this.targets = targets;
        }

        @Override
        public CompletableFuture<Void> send(P payload) {
            Objects.requireNonNull(payload);
            UUID requirePlayer = PluginMessageChannel.this
                    .resolveConditionUuid(payload, requirePlayerUuid, requirePlayerFromPayload);
            UUID whenOnline = PluginMessageChannel.this.resolveConditionUuid(payload, whenOnlineUuid, whenOnlineFromPayload);
            PluginMessage message = toPluginMessage(payload, false, requirePlayer, whenOnline);
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

        @Override
        public Sender<P> requirePlayer(UUID player) {
            Objects.requireNonNull(player);
            if (whenOnlineUuid != null || whenOnlineFromPayload) {
                throw new IllegalStateException("Cannot combine requirePlayer and whenOnline");
            }
            requirePlayerUuid = player;
            requirePlayerFromPayload = false;
            return this;
        }

        @Override
        public Sender<P> requirePlayer() {
            if (whenOnlineUuid != null || whenOnlineFromPayload) {
                throw new IllegalStateException("Cannot combine requirePlayer and whenOnline");
            }
            requirePlayerFromPayload = true;
            requirePlayerUuid = null;
            return this;
        }

        @Override
        public Sender<P> whenOnline(UUID player) {
            Objects.requireNonNull(player);
            if (requirePlayerUuid != null || requirePlayerFromPayload) {
                throw new IllegalStateException("Cannot combine requirePlayer and whenOnline");
            }
            whenOnlineUuid = player;
            whenOnlineFromPayload = false;
            return this;
        }

        @Override
        public Sender<P> whenOnline() {
            if (requirePlayerUuid != null || requirePlayerFromPayload) {
                throw new IllegalStateException("Cannot combine requirePlayer and whenOnline");
            }
            whenOnlineFromPayload = true;
            whenOnlineUuid = null;
            return this;
        }
    }

    private final class BroadcastSender implements Sender<P> {

        @Override
        public CompletableFuture<Void> send(P payload) {
            Objects.requireNonNull(payload);
            return sendTransport.send(Platform.BACKEND.target("*"), toPluginMessage(payload, false));
        }

        @Override
        public CompletableFuture<P> request(P payload) {
            throw new UnsupportedOperationException("Request is not supported for broadcast senders");
        }

        @Override
        public CompletableFuture<P> request(P payload, Duration timeout) {
            throw new UnsupportedOperationException("Request is not supported for broadcast senders");
        }

        @Override
        public Sender<P> requirePlayer(UUID player) {
            throw new UnsupportedOperationException("Delivery conditions are not supported for broadcast senders");
        }

        @Override
        public Sender<P> requirePlayer() {
            throw new UnsupportedOperationException("Delivery conditions are not supported for broadcast senders");
        }

        @Override
        public Sender<P> whenOnline(UUID player) {
            throw new UnsupportedOperationException("Delivery conditions are not supported for broadcast senders");
        }

        @Override
        public Sender<P> whenOnline() {
            throw new UnsupportedOperationException("Delivery conditions are not supported for broadcast senders");
        }
    }
}
