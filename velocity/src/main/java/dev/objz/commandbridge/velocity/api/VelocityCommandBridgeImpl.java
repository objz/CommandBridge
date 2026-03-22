package dev.objz.commandbridge.velocity.api;

import dev.objz.commandbridge.api.CommandBridgeAPI;
import dev.objz.commandbridge.api.channel.ChannelPayload;
import dev.objz.commandbridge.api.channel.ChannelType;
import dev.objz.commandbridge.api.channel.MessageChannel;
import dev.objz.commandbridge.api.channel.command.CommandChannelType;
import dev.objz.commandbridge.api.message.MessageContext;
import dev.objz.commandbridge.api.message.MessageListener;
import dev.objz.commandbridge.api.message.ServerEventListener;
import dev.objz.commandbridge.api.message.Subscription;
import dev.objz.commandbridge.api.platform.ConnectionState;
import dev.objz.commandbridge.api.platform.Platform;
import dev.objz.commandbridge.api.platform.PlayerLocator;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.channel.CommandMessageChannel;
import dev.objz.commandbridge.net.channel.PluginMessageChannel;
import dev.objz.commandbridge.net.payloads.PluginMessage;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.net.EndpointServer;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.util.PlayerTracker;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

public final class VelocityCommandBridgeImpl implements CommandBridgeAPI {

    private final SessionHub sessions;
    private final PlayerTracker playerTracker;
    private final String serverId;
    private final EndpointServer endpointServer;

    private final ConcurrentHashMap<Class<?>, MessageChannel<?>> channels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ChannelType<?, ?>> channelTypesByName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<MessageListener<?>>> listenersByType = new ConcurrentHashMap<>();
    private final CopyOnWriteArraySet<ServerEventListener> connectedListeners = new CopyOnWriteArraySet<>();
    private final CopyOnWriteArraySet<ServerEventListener> disconnectedListeners = new CopyOnWriteArraySet<>();

    public VelocityCommandBridgeImpl(SessionHub sessions,
            PlayerTracker playerTracker,
            String serverId,
            EndpointServer endpointServer) {
        this.sessions = Objects.requireNonNull(sessions);
        this.playerTracker = Objects.requireNonNull(playerTracker);
        this.serverId = Objects.requireNonNull(serverId);
        this.endpointServer = Objects.requireNonNull(endpointServer);
    }

    @Override
    public <T extends ChannelPayload, C extends MessageChannel<T>> C channel(ChannelType<T, C> type) {
        Objects.requireNonNull(type);
        channelTypesByName.putIfAbsent(type.getClass().getName(), type);
        MessageChannel<?> channel = channels.computeIfAbsent(type.getClass(), ignored -> createChannel(type));
        return typeCast(channel);
    }

    @Override
    public Platform.ServerTarget server() {
        return Platform.VELOCITY.target(serverId);
    }

    @Override
    public ConnectionState connectionState() {
        return ConnectionState.AUTHENTICATED;
    }

    @Override
    public Optional<Set<String>> connectedServers() {
        Set<String> result = new LinkedHashSet<>();
        for (ClientSession session : sessions) {
            if (isRoutable(session)) {
                result.add(session.id());
            }
        }
        return Optional.of(Set.copyOf(result));
    }

    @Override
    public Optional<PlayerLocator> playerLocator() {
        return Optional.of(player -> {
            for (ClientSession session : sessions) {
                if (isRoutable(session) && playerTracker.isPlayerOn(player, session.id())) {
                    return Optional.of(toPlatform(session.location()).target(session.id()));
                }
            }
            return Optional.empty();
        });
    }

    @Override
    public Subscription onServerConnected(ServerEventListener listener) {
        Objects.requireNonNull(listener);
        connectedListeners.add(listener);
        return () -> connectedListeners.remove(listener);
    }

    @Override
    public Subscription onServerDisconnected(ServerEventListener listener) {
        Objects.requireNonNull(listener);
        disconnectedListeners.add(listener);
        return () -> disconnectedListeners.remove(listener);
    }

    @Override
    public Subscription onConnectionStateChanged(Consumer<ConnectionState> listener) {
        Objects.requireNonNull(listener);
        listener.accept(ConnectionState.AUTHENTICATED);
        return () -> {
        };
    }

    public void onServerConnected(ClientSession session) {
        if (session == null || !isRoutable(session)) {
            return;
        }
        Platform.ServerTarget target = toPlatform(session.location()).target(session.id());
        for (ServerEventListener listener : connectedListeners) {
            try {
                listener.accept(target);
            } catch (Exception e) {
                Log.warn("Failed to run server-connect listener: {}", e.getMessage());
            }
        }
    }

    public void onServerDisconnected(ClientSession session) {
        if (session == null || session.id() == null || session.id().isBlank()) {
            return;
        }
        Platform.ServerTarget target = toPlatform(session.location()).target(session.id());
        for (ServerEventListener listener : disconnectedListeners) {
            try {
                listener.accept(target);
            } catch (Exception e) {
                Log.warn("Failed to run server-disconnect listener: {}", e.getMessage());
            }
        }
    }

    public void handlePluginMessageRequest(Endpoint endpoint, Envelope env) {
        String to = env.to();
        if (to != null && !to.equals(serverId) && !"*".equals(to)) {
            relayDirect(env);
            return;
        }

        PluginMessage message = readPluginMessage(env);
        if (message == null) {
            return;
        }

        dispatchLocal(env, message);

        if ("*".equals(to)) {
            relayBroadcast(env);
            return;
        }

        if (message.expectsResponse()) {
            Envelope response = Envelope.reply(env, MessageType.PLUGIN_MESSAGE_RESPONSE, serverId,
                    Envelope.MAPPER.valueToTree(message));
            endpointServer.send(endpoint, response).dispatch().exceptionally(ex -> {
                Log.warn("Failed to send plugin message response: {}", ex.getMessage());
                return null;
            });
        }
    }

    public void handlePluginMessageResponse(Envelope env) {
        String to = env.to();
        if (to != null && !to.equals(serverId) && !"*".equals(to)) {
            relayDirect(env);
        }
    }

    private <T extends ChannelPayload, C extends MessageChannel<T>> MessageChannel<?> createChannel(ChannelType<T, C> type) {
        if (type instanceof CommandChannelType commandType) {
            return new CommandMessageChannel(commandType,
                    this::sendPluginMessage,
                    this::requestPluginMessage,
                    listener -> registerListener(commandType, listener));
        }

        return new PluginMessageChannel<>(type,
                this::sendPluginMessage,
                this::requestPluginMessage,
                listener -> registerListener(type, listener));
    }

    private <P extends ChannelPayload> Subscription registerListener(ChannelType<P, ? extends MessageChannel<P>> type,
            MessageListener<P> listener) {
        String key = type.getClass().getName();
        channelTypesByName.putIfAbsent(key, type);
        CopyOnWriteArrayList<MessageListener<?>> listeners = listenersByType.computeIfAbsent(key,
                ignored -> new CopyOnWriteArrayList<>());
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    private CompletableFuture<Void> sendPluginMessage(Platform.ServerTarget target, PluginMessage payload) {
        if ("*".equals(target.id())) {
            return broadcastPluginMessage(payload);
        }

        if (isLocalVelocity(target)) {
            Envelope local = Envelope.make(MessageType.PLUGIN_MESSAGE, serverId, serverId,
                    Envelope.MAPPER.valueToTree(payload));
            dispatchLocal(local, payload);
            return CompletableFuture.completedFuture(null);
        }

        Optional<ClientSession> session = sessions.findSession(target.id(), toLocation(target.type()));
        if (session.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Target server is not connected: " + target.id()));
        }

        Envelope env = Envelope.make(MessageType.PLUGIN_MESSAGE, serverId, target.id(),
                Envelope.MAPPER.valueToTree(payload));
        return endpointServer.send(session.get().endpoint(), env).dispatch();
    }

    private CompletableFuture<Void> broadcastPluginMessage(PluginMessage payload) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (ClientSession session : sessions) {
            if (!isRoutable(session)) {
                continue;
            }
            Envelope env = Envelope.make(MessageType.PLUGIN_MESSAGE, serverId, session.id(),
                    Envelope.MAPPER.valueToTree(payload));
            futures.add(endpointServer.send(session.endpoint(), env).dispatch());
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<PluginMessage> requestPluginMessage(Platform.ServerTarget target,
            PluginMessage payload,
            Duration timeout) {
        Objects.requireNonNull(timeout);
        if (isLocalVelocity(target)) {
            Envelope local = Envelope.make(MessageType.PLUGIN_MESSAGE, serverId, serverId,
                    Envelope.MAPPER.valueToTree(payload));
            dispatchLocal(local, payload);
            return CompletableFuture.completedFuture(payload);
        }

        Optional<ClientSession> session = sessions.findSession(target.id(), toLocation(target.type()));
        if (session.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Target server is not connected: " + target.id()));
        }

        Envelope env = Envelope.make(MessageType.PLUGIN_MESSAGE, serverId, target.id(),
                Envelope.MAPPER.valueToTree(payload));
        return endpointServer.send(session.get().endpoint(), env)
                .expect(MessageType.PLUGIN_MESSAGE_RESPONSE)
                .timeout(timeout)
                .await()
                .thenApply(this::requirePluginMessage);
    }

    private void dispatchLocal(Envelope env, PluginMessage message) {
        ChannelType<?, ?> channelType = channelTypesByName.get(message.channelType());
        if (channelType == null) {
            return;
        }

        Object payload;
        try {
            payload = Envelope.MAPPER.treeToValue(message.data(), channelType.type());
        } catch (Exception e) {
            Log.warn("Failed to decode plugin payload for {}: {}", message.channelType(), e.getMessage());
            return;
        }

        CopyOnWriteArrayList<MessageListener<?>> listeners = listenersByType.get(message.channelType());
        if (listeners == null || listeners.isEmpty()) {
            return;
        }

        Platform.ServerTarget source = sourceTarget(env.from());
        MessageContext<ChannelPayload> context = contextCast(new MessageContext<>(
                typeCast(channelType),
                source,
                env.ts()));

        for (MessageListener<?> raw : listeners) {
            try {
                listenerCast(raw).accept(context, payloadCast(payload));
            } catch (Exception e) {
                Log.warn("Plugin message listener failed for {}: {}", message.channelType(), e.getMessage());
            }
        }
    }

    private void relayDirect(Envelope env) {
        if (env.to() == null || env.to().isBlank()) {
            return;
        }

        Optional<ClientSession> target = sessions.get(env.to());
        if (target.isEmpty() || !isRoutable(target.get())) {
            return;
        }

        endpointServer.send(target.get().endpoint(), env).dispatch().exceptionally(ex -> {
            Log.warn("Failed to relay plugin message to {}: {}", env.to(), ex.getMessage());
            return null;
        });
    }

    private void relayBroadcast(Envelope env) {
        for (ClientSession session : sessions) {
            if (!isRoutable(session) || session.id().equals(env.from())) {
                continue;
            }

            Envelope forwarded = new Envelope(env.v(), env.id(), env.type(), env.from(), session.id(), env.ts(),
                    env.payload());
            endpointServer.send(session.endpoint(), forwarded).dispatch().exceptionally(ex -> {
                Log.warn("Failed to relay broadcast plugin message to {}: {}", session.id(), ex.getMessage());
                return null;
            });
        }
    }

    private PluginMessage readPluginMessage(Envelope env) {
        try {
            return Envelope.MAPPER.treeToValue(env.payload(), PluginMessage.class);
        } catch (Exception e) {
            Log.warn("Failed to decode plugin message: {}", e.getMessage());
            return null;
        }
    }

    private PluginMessage requirePluginMessage(Envelope env) {
        try {
            return Envelope.MAPPER.treeToValue(env.payload(), PluginMessage.class);
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    private Platform.ServerTarget sourceTarget(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return Platform.BACKEND.target("unknown");
        }
        if (serverId.equals(sourceId)) {
            return Platform.VELOCITY.target(sourceId);
        }

        return sessions.get(sourceId)
                .map(session -> toPlatform(session.location()).target(sourceId))
                .orElse(Platform.BACKEND.target(sourceId));
    }

    private boolean isLocalVelocity(Platform.ServerTarget target) {
        return target.type() == Platform.VELOCITY && serverId.equals(target.id());
    }

    private boolean isRoutable(ClientSession session) {
        return session != null
                && session.id() != null
                && !session.id().isBlank()
                && session.status() == AuthStatus.AUTH_OK
                && session.endpoint() != null
                && session.endpoint().isOpen();
    }

    private static Platform toPlatform(Location location) {
        return location == Location.VELOCITY ? Platform.VELOCITY : Platform.BACKEND;
    }

    private static Location toLocation(Platform platform) {
        return platform == Platform.VELOCITY ? Location.VELOCITY : Location.BACKEND;
    }

    @SuppressWarnings("unchecked")
    private static <T extends ChannelPayload, C extends MessageChannel<T>> C typeCast(Object value) {
        return (C) value;
    }

    @SuppressWarnings("unchecked")
    private static MessageContext<ChannelPayload> contextCast(MessageContext<?> context) {
        return (MessageContext<ChannelPayload>) context;
    }

    @SuppressWarnings("unchecked")
    private static MessageListener<ChannelPayload> listenerCast(MessageListener<?> listener) {
        return (MessageListener<ChannelPayload>) listener;
    }

    private static ChannelPayload payloadCast(Object value) {
        return (ChannelPayload) value;
    }
}
