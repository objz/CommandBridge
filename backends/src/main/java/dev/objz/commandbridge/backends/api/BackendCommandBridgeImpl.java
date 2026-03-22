package dev.objz.commandbridge.backends.api;

import dev.objz.commandbridge.api.CommandBridgeAPI;
import dev.objz.commandbridge.api.CommandBridgeProvider;
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
import dev.objz.commandbridge.backends.net.client.BackendClient;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.channel.CommandMessageChannel;
import dev.objz.commandbridge.net.channel.PluginMessageChannel;
import dev.objz.commandbridge.net.payloads.PluginMessage;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class BackendCommandBridgeImpl implements CommandBridgeAPI {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    private final BackendClient client;
    private final ConcurrentHashMap<Class<?>, MessageChannel<?>> channels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ChannelType<?, ?>> channelTypesByName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<MessageListener<?>>> listenersByType = new ConcurrentHashMap<>();
    private final CopyOnWriteArraySet<Consumer<ConnectionState>> stateListeners = new CopyOnWriteArraySet<>();
    private volatile ScheduledExecutorService statePoller;
    private final AtomicReference<ConnectionState> lastState = new AtomicReference<>(ConnectionState.DISCONNECTED);
    private final AtomicBoolean polling = new AtomicBoolean(false);
    private final AtomicBoolean providerRegistered = new AtomicBoolean(false);

    public BackendCommandBridgeImpl(BackendClient client) {
        this.client = Objects.requireNonNull(client);
    }

    public void bootstrap() {
        client.inboundRouter().register(MessageType.PLUGIN_MESSAGE, new BackendPluginMessageHandler(this, false));
        client.inboundRouter().register(MessageType.PLUGIN_MESSAGE_RESPONSE,
                new BackendPluginMessageHandler(this, true));
        if (providerRegistered.compareAndSet(false, true)) {
            CommandBridgeProvider.register(this);
        }
    }

    public void shutdown() {
        if (providerRegistered.compareAndSet(true, false)) {
            CommandBridgeProvider.unregister();
        }
        polling.set(false);
        if (statePoller != null) {
            statePoller.shutdownNow();
            statePoller = null;
        }
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
        return Platform.BACKEND.target(localServerId());
    }

    @Override
    public ConnectionState connectionState() {
        return client.status().toConnectionState();
    }

    @Override
    public Optional<Set<String>> connectedServers() {
        return Optional.empty();
    }

    @Override
    public Optional<PlayerLocator> playerLocator() {
        return Optional.empty();
    }

    @Override
    public Subscription onServerConnected(ServerEventListener listener) {
        Objects.requireNonNull(listener);
        return () -> { };
    }

    @Override
    public Subscription onServerDisconnected(ServerEventListener listener) {
        Objects.requireNonNull(listener);
        return () -> { };
    }

    @Override
    public Subscription onConnectionStateChanged(Consumer<ConnectionState> listener) {
        Objects.requireNonNull(listener);
        stateListeners.add(listener);
        listener.accept(connectionState());
        startStatePolling();
        return () -> stateListeners.remove(listener);
    }

    public PluginMessage handlePluginMessageRequest(Envelope env) {
        String to = env.to();
        String localId = localServerId();
        if (to != null && !to.equals(localId) && !"*".equals(to)) {
            return null;
        }

        PluginMessage message = readPluginMessage(env);
        if (message == null) {
            return null;
        }

        dispatchLocal(env, message);
        if ("*".equals(to)) {
            return null;
        }
        return message;
    }

    public void handlePluginMessageResponse(Envelope env) {
        String to = env.to();
        if (to != null && !to.equals(localServerId()) && !"*".equals(to)) {
            Log.warn("Dropping plugin response for mismatched target {} (local={})", to, localServerId());
        }
    }

    private <T extends ChannelPayload, C extends MessageChannel<T>> MessageChannel<?> createChannel(
            ChannelType<T, C> type) {
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
        Envelope env = Envelope.make(MessageType.PLUGIN_MESSAGE, localServerId(), target.id(),
                Envelope.MAPPER.valueToTree(payload));
        return client.send(env).dispatch();
    }

    private CompletableFuture<PluginMessage> requestPluginMessage(Platform.ServerTarget target,
            PluginMessage payload,
            Duration timeout) {
        Duration effectiveTimeout = timeout != null ? timeout : DEFAULT_TIMEOUT;
        Envelope env = Envelope.make(MessageType.PLUGIN_MESSAGE, localServerId(), target.id(),
                Envelope.MAPPER.valueToTree(payload));
        return client.send(env)
                .expect(MessageType.PLUGIN_MESSAGE_RESPONSE)
                .timeout(effectiveTimeout)
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

        if (env.from() == null || env.from().isBlank()) {
            Log.warn("Received plugin message with missing source server ID");
            return;
        }
        Platform.ServerTarget source = Platform.BACKEND.target(env.from());
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

    private String localServerId() {
        String id = client.serverId();
        return (id == null || id.isBlank()) ? "backend" : id;
    }

    private void startStatePolling() {
        if (!polling.compareAndSet(false, true)) {
            return;
        }

        statePoller = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "commandbridge-api-state");
            thread.setDaemon(true);
            return thread;
        });

        lastState.set(connectionState());
        statePoller.scheduleAtFixedRate(() -> {
            try {
                ConnectionState current = connectionState();
                ConnectionState previous = lastState.getAndSet(current);
                if (current != previous) {
                    for (Consumer<ConnectionState> listener : stateListeners) {
                        try {
                            listener.accept(current);
                        } catch (Exception e) {
                            Log.warn("Failed to run state-change listener: {}", e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                Log.warn("State polling failed: {}", e.getMessage());
            }
        }, 500L, 500L, TimeUnit.MILLISECONDS);
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
