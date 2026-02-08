package dev.objz.commandbridge.backends.net;

import dev.objz.commandbridge.backends.platform.PlatformAdapter;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.config.model.TlsMode;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.InNode;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.ResponseAwaiter;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.security.SecretLoader;
import io.undertow.websockets.core.WebSocketChannel;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class WsClient implements AutoCloseable {
    private final BackendsConfig cfg;
    private final Path dataDir;

    // idk about this
    private final PlatformAdapter adapter;

    private final ResourcePool resources;
    private final ConnectionHandler connectionHandler;
    private final ReconnectHandler reconnectHandler;
    private final MessageRouter messageRouter;
    private final AuthHandler authHandler;

    private final InNode inNode = new InNode();
    private final OutNode<Object> outNode = new OutNode<>();
    private final ResponseAwaiter awaiter = new ResponseAwaiter();

    private final AtomicReference<ConnectionState> stateRef = new AtomicReference<>(ConnectionState.DISCONNECTED);
    private Location location = Location.BACKEND;
    private String serverId;

    public WsClient(BackendsConfig cfg, Path dataDir, PlatformAdapter adapter) {
        this.cfg = Objects.requireNonNull(cfg);
        this.dataDir = Objects.requireNonNull(dataDir);
        this.adapter = Objects.requireNonNull(adapter);
        this.resources = new ResourcePool();
        this.connectionHandler = new ConnectionHandler(cfg, resources);
        this.reconnectHandler = new ReconnectHandler(cfg, adapter, this::attemptReconnect);
        this.authHandler = new AuthHandler(cfg, outNode, stateRef);

        String secret = resolveSecret();
        this.messageRouter = new MessageRouter(
                inNode,
                outNode,
                awaiter,
                stateRef,
                this::onConnectionLost,
                secret,
                location);

        outNode.setClientId(cfg.clientId());
    }

    public synchronized void start() throws Exception {
        if (connectionHandler.isConnected()) {
            Log.debug("Already connected, skipping start");
            return;
        }

        if (!reconnectHandler.isReconnecting()) {
            reconnectHandler.stopReconnect();
        }

        try {
            stateRef.set(ConnectionState.CONNECTING);

            resources.initialize();

            TlsMode mode = (cfg.security() != null && cfg.security().tlsMode() != null)
                    ? cfg.security().tlsMode()
                    : TlsMode.TOFU;

            String configuredPin = (cfg.security() != null) ? cfg.security().tlsPin() : null;
            resources.initializeSsl(mode, configuredPin, reconnectHandler.isReconnecting());

            WebSocketChannel channel = connectionHandler.connect(reconnectHandler.isReconnecting());
            stateRef.set(ConnectionState.CONNECTED);

            messageRouter.setupChannel(channel);

            authHandler.authenticate(channel);

            if (reconnectHandler.isReconnecting()) {
                reconnectHandler.onReconnectSuccess();
            }

        } catch (Exception e) {
            stateRef.set(ConnectionState.DISCONNECTED);

            if (!reconnectHandler.isReconnecting()) {
                Log.warn("Connection failed: {}", e.getMessage());
            }

            throw e;
        }
    }

    public synchronized void reconnect() throws Exception {
        Log.warn("Manual reconnection initiated");
        reconnectHandler.shutdown();
        messageRouter.clearTap();
        connectionHandler.forceClose();
        stateRef.set(ConnectionState.DISCONNECTED);
        resources.close();
        start();
    }

    public void scheduleReconnection() {
        reconnectHandler.scheduleReconnect();
    }

    public SendOperation send(Envelope request) {
        if (!connectionHandler.isChannelHealthy()) {
            throw new IllegalStateException("WebSocket not connected or unhealthy");
        }

        WebSocketChannel channel = connectionHandler.getChannel();
        if (channel == null) {
            throw new IllegalStateException("WebSocket channel is null");
        }

        return new SendOperation(channel, request, awaiter);
    }

    @Override
    public synchronized void close() throws Exception {
        Log.debug("Closing WsClient");

        reconnectHandler.shutdown();

        try {
            messageRouter.clearTap();

            ConnectionState currentState = stateRef.get();
            connectionHandler.disconnect(currentState);

        } finally {
            stateRef.set(ConnectionState.DISCONNECTED);

            resources.close();
        }

        Log.debug("WsClient closed");
    }

    public ClientStatus status() {
        return stateRef.get().toClientStatus();
    }

    public String serverId() {
        return serverId;
    }

    public InNode inboundRouter() {
        return inNode;
    }

    public OutNode<Object> outboundRouter() {
        return outNode;
    }

    public void setLocation(Location location) {
        this.location = Objects.requireNonNull(location);
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
        outNode.setServerId(serverId);
    }

    private void onConnectionLost() {
        stateRef.set(ConnectionState.RECONNECTING);
        connectionHandler.forceClose();
        reconnectHandler.scheduleReconnect();
    }

    private void attemptReconnect() {
        try {
            if (connectionHandler.isConnected()) {
                connectionHandler.forceClose();
            }

            resources.close();

            start();

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String resolveSecret() {
        String s = cfg.security() != null ? cfg.security().secret() : null;
        if (s != null && !s.isBlank()) {
            return s;
        }
        return new SecretLoader(dataDir).loadOrCreate();
    }
}
