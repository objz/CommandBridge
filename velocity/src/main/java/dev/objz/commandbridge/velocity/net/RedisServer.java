package dev.objz.commandbridge.velocity.net;

import com.fasterxml.jackson.databind.JsonNode;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.InNode;
import dev.objz.commandbridge.net.ResponseAwaiter;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.endpoints.RedisEndpoint;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.redis.RedisChannels;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class RedisServer implements EndpointServer {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final SessionHub sessions;
    private final InNode inNode;
    private final ResponseAwaiter responses = new ResponseAwaiter();
    private final Map<String, RedisEndpoint> endpointsByClient = new ConcurrentHashMap<>();

    private volatile boolean running;
    private volatile JedisPool pool;
    private volatile JedisPubSub subscriber;
    private volatile Thread subscriberThread;

    public RedisServer(
            String host,
            int port,
            String username,
            String password,
            SessionHub sessions,
            InNode inNode) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.sessions = sessions;
        this.inNode = inNode;

        this.inNode.setInboundTap(this::signalInbound);
        this.inNode.setSendOperationFactory(this::createSendOperation);
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }

        try {
            pool = createPool();
            running = true;
            startSubscriber();
            Log.success(true, "Redis endpoint listening via '{}:{}'", host, port);
        } catch (Exception e) {
            running = false;
            closePool();
            throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
        }
    }

    @Override
    public synchronized void stop() {
        running = false;

        JedisPubSub activeSub = subscriber;
        if (activeSub != null) {
            try {
                activeSub.unsubscribe();
            } catch (Exception ignore) {
            }
        }

        Thread t = subscriberThread;
        if (t != null) {
            t.interrupt();
            try {
                t.join(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        closePool();
        endpointsByClient.clear();
        sessions.clear();
        Log.info("Redis endpoint server stopped");
    }

    @Override
    public SendOperation send(Endpoint endpoint, Envelope request) {
        return new SendOperation(endpoint, request, responses);
    }

    @Override
    public void close(Endpoint endpoint) {
        if (endpoint == null) {
            return;
        }
        sessions.remove(endpoint);
        if (endpoint instanceof RedisEndpoint redisEndpoint) {
            endpointsByClient.remove(redisEndpoint.id(), redisEndpoint);
        }
    }

    private SendOperation createSendOperation(Endpoint endpoint, Envelope env) {
        return new SendOperation(endpoint, env, responses);
    }

    private void startSubscriber() {
        subscriberThread = new Thread(this::subscribeLoop, "commandbridge-redis-proxy-sub");
        subscriberThread.setDaemon(true);
        subscriberThread.start();
    }

    private void subscribeLoop() {
        while (running) {
            try (Jedis jedis = pool.getResource()) {
                JedisPubSub localSubscriber = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        handleInbound(message);
                    }
                };
                subscriber = localSubscriber;
                jedis.subscribe(localSubscriber, RedisChannels.PROXY_INBOUND);
            } catch (Exception ex) {
                if (!running) {
                    return;
                }
                Log.warn("Redis subscribe loop failed: {}", ex.getMessage());
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } finally {
                subscriber = null;
            }
        }
    }

    private void handleInbound(String message) {
        String clientId = parseFrom(message);
        if (clientId == null || clientId.isBlank()) {
            Log.warn("Dropping Redis message without a valid 'from' client id");
            return;
        }

        RedisEndpoint endpoint = endpointsByClient.computeIfAbsent(clientId,
                id -> new RedisEndpoint(id, env -> publishToClient(id, env), () -> running));
        inNode.onText(endpoint, message);
    }

    private String parseFrom(String text) {
        try {
            JsonNode node = Envelope.MAPPER.readTree(text);
            JsonNode from = node.get("from");
            return from != null ? from.asText() : null;
        } catch (Exception e) {
            Log.warn("Bad JSON from Redis: {}", e.getMessage());
            return null;
        }
    }

    private CompletableFuture<Void> publishToClient(String clientId, Envelope env) {
        try {
            String payload = Envelope.MAPPER.writeValueAsString(env);
            try (Jedis jedis = pool.getResource()) {
                jedis.publish(RedisChannels.clientInbound(clientId), payload);
            }
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    private boolean signalInbound(Envelope env) {
        try {
            return responses.signal(env);
        } catch (Exception ex) {
            Log.debug("Awaiter signal failed: {}", ex.toString());
            return false;
        }
    }

    private JedisPool createPool() {
        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder();
        if (username != null && !username.isBlank()) {
            builder.user(username.trim());
        }
        if (password != null && !password.isBlank()) {
            builder.password(password);
        }
        return new JedisPool(new HostAndPort(host, port), builder.build());
    }

    private void closePool() {
        JedisPool p = pool;
        pool = null;
        if (p != null) {
            try {
                p.close();
            } catch (Exception ignore) {
            }
        }
    }
}
