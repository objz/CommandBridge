package dev.objz.commandbridge.backends.net.client;

import dev.objz.commandbridge.backends.net.connection.ClientStatus;
import dev.objz.commandbridge.backends.net.connection.ConnectionState;
import dev.objz.commandbridge.backends.net.connection.ReconnectHandler;
import dev.objz.commandbridge.backends.net.routing.AuthHandler;
import dev.objz.commandbridge.backends.net.routing.RedisMessageRouter;
import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.InNode;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.ResponseAwaiter;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.endpoints.RedisEndpoint;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.redis.RedisChannels;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.security.SecretLoader;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class RedisClient implements BackendClient {
    private final BackendsConfig cfg;
    private final Path dataDir;
    private final PlatformAdapter adapter;

    private final ReconnectHandler reconnectHandler;
    private final AuthHandler authHandler;
    private final RedisMessageRouter messageRouter;

    private final InNode inNode = new InNode();
    private final OutNode<Object> outNode = new OutNode<>();
    private final ResponseAwaiter awaiter = new ResponseAwaiter();
    private final AtomicReference<ConnectionState> stateRef = new AtomicReference<>(ConnectionState.DISCONNECTED);

    private volatile JedisPool pool;
    private volatile JedisPubSub subscriber;
    private volatile Thread subscriberThread;
    private volatile boolean running;
    private volatile RedisEndpoint proxyEndpoint;

    private Location location = Location.BACKEND;
    private String serverId;

    public RedisClient(BackendsConfig cfg, Path dataDir, PlatformAdapter adapter) {
        this.cfg = Objects.requireNonNull(cfg);
        this.dataDir = Objects.requireNonNull(dataDir);
        this.adapter = Objects.requireNonNull(adapter);

        this.reconnectHandler = new ReconnectHandler(cfg, adapter, this::attemptReconnect);
        this.authHandler = new AuthHandler(cfg, outNode, stateRef);
        this.messageRouter = new RedisMessageRouter(
                inNode,
                outNode,
                awaiter,
                stateRef,
                resolveSecret(),
                location);

        outNode.setClientId(cfg.clientId());
    }

    @Override
    public synchronized void start() throws Exception {
        if (isConnected()) {
            Log.debug("Already connected, skipping start");
            return;
        }

        if (!reconnectHandler.isReconnecting()) {
            reconnectHandler.stopReconnect();
        }

        try {
            stateRef.set(ConnectionState.CONNECTING);
            running = true;
            pool = createPool();

            proxyEndpoint = new RedisEndpoint("proxy", this::publishToProxy, this::isConnected);
            messageRouter.setupEndpoint(proxyEndpoint);
            startSubscriber();

            stateRef.set(ConnectionState.CONNECTED);
            authHandler.authenticate();

            if (reconnectHandler.isReconnecting()) {
                reconnectHandler.onReconnectSuccess();
            }
        } catch (Exception e) {
            stateRef.set(ConnectionState.DISCONNECTED);
            running = false;
            stopInternal();
            if (!reconnectHandler.isReconnecting()) {
                Log.warn("Redis connection failed: {}", e.getMessage());
            }
            throw e;
        }
    }

    @Override
    public synchronized void reconnect() throws Exception {
        Log.warn("Manual reconnection initiated");
        reconnectHandler.shutdown();
        messageRouter.clearTap();
        stopInternal();
        stateRef.set(ConnectionState.DISCONNECTED);
        start();
    }

    @Override
    public void scheduleReconnection() {
        reconnectHandler.scheduleReconnect();
    }

    @Override
    public SendOperation send(Envelope request) {
        if (!isConnected() || proxyEndpoint == null) {
            throw new IllegalStateException("Redis endpoint not connected");
        }
        return new SendOperation(proxyEndpoint, request, awaiter);
    }

    @Override
    public synchronized void close() {
        Log.debug("Closing RedisClient");
        reconnectHandler.shutdown();
        messageRouter.clearTap();
        stopInternal();
        stateRef.set(ConnectionState.DISCONNECTED);
        Log.debug("RedisClient closed");
    }

    @Override
    public ClientStatus status() {
        return stateRef.get().toClientStatus();
    }

    @Override
    public String serverId() {
        return serverId;
    }

    @Override
    public InNode inboundRouter() {
        return inNode;
    }

    @Override
    public OutNode<Object> outboundRouter() {
        return outNode;
    }

    @Override
    public void setLocation(Location location) {
        this.location = Objects.requireNonNull(location);
        this.messageRouter.setLocation(this.location);
    }

    @Override
    public void setServerId(String serverId) {
        this.serverId = serverId;
        outNode.setServerId(serverId);
    }

    @Override
    public void onAuthenticated(Runnable callback) {
        authHandler.onAuthenticated(callback);
    }

    private boolean isConnected() {
        return running && pool != null;
    }

    private void onConnectionLost() {
        if (!running) {
            return;
        }
        stateRef.set(ConnectionState.RECONNECTING);
        stopInternal();
        reconnectHandler.scheduleReconnect();
    }

    private void attemptReconnect() {
        try {
            stopInternal();
            start();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void startSubscriber() throws Exception {
        CountDownLatch subscribed = new CountDownLatch(1);
        subscriberThread = new Thread(() -> subscribeLoop(subscribed), "commandbridge-redis-client-sub");
        subscriberThread.setDaemon(true);
        subscriberThread.start();

        if (!subscribed.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for Redis subscription");
        }
    }

    private void subscribeLoop(CountDownLatch subscribed) {
        try (Jedis jedis = pool.getResource()) {
            JedisPubSub localSubscriber = new JedisPubSub() {
                @Override
                public void onSubscribe(String channel, int subscribedChannels) {
                    subscribed.countDown();
                }

                @Override
                public void onMessage(String channel, String message) {
                    RedisEndpoint endpoint = proxyEndpoint;
                    if (endpoint != null) {
                        messageRouter.onText(endpoint, message);
                    }
                }
            };

            subscriber = localSubscriber;
            jedis.subscribe(localSubscriber, RedisChannels.clientInbound(cfg.clientId()));
        } catch (Exception e) {
            subscribed.countDown();
            if (running) {
                Log.warn("Redis subscriber disconnected: {}", e.getMessage());
                onConnectionLost();
            }
        } finally {
            subscriber = null;
        }
    }

    private CompletableFuture<Void> publishToProxy(Envelope env) {
        try {
            String payload = Envelope.MAPPER.writeValueAsString(env);
            try (Jedis jedis = pool.getResource()) {
                jedis.publish(RedisChannels.PROXY_INBOUND, payload);
            }
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            if (running) {
                Log.warn("Redis publish failed: {}", e.getMessage());
                onConnectionLost();
            }
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    private JedisPool createPool() {
        BackendsConfig.Endpoints.Redis redis = cfg.endpoints().redis();
        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder();
        if (redis.username() != null && !redis.username().isBlank()) {
            builder.user(redis.username().trim());
        }
        if (redis.password() != null && !redis.password().isBlank()) {
            builder.password(redis.password());
        }
        return new JedisPool(new HostAndPort(redis.host(), redis.port()), builder.build());
    }

    private void stopInternal() {
        running = false;

        JedisPubSub sub = subscriber;
        if (sub != null) {
            try {
                sub.unsubscribe();
            } catch (Exception ignore) {
            }
        }

        Thread t = subscriberThread;
        subscriberThread = null;
        if (t != null) {
            t.interrupt();
            try {
                t.join(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        JedisPool p = pool;
        pool = null;
        if (p != null) {
            try {
                p.close();
            } catch (Exception e) {
                Log.warn("Failed to close Redis pool: {}", e.getMessage());
            }
        }
    }

    private String resolveSecret() {
        String configSecret = cfg.security() != null ? cfg.security().secret() : null;
        return SecretLoader.resolve(configSecret, dataDir);
    }
}
