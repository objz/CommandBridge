package dev.objz.commandbridge.backends.net;

import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.config.model.TlsMode;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.security.TlsResolver;
import io.undertow.websockets.client.WebSocketClient;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.xnio.IoFuture;
import org.xnio.IoUtils;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class ConnectionHandler {
    private final BackendsConfig cfg;
    private final ResourcePool resources;
    private final AtomicReference<WebSocketChannel> channelRef = new AtomicReference<>();
    private final ClassLoader pluginClassLoader;

    public ConnectionHandler(BackendsConfig cfg, ResourcePool resources) {
        this.cfg = cfg;
        this.resources = resources;
        this.pluginClassLoader = getClass().getClassLoader();
    }

    public WebSocketChannel connect(boolean isReconnecting) throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(pluginClassLoader);
            return connectInternal(isReconnecting);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private WebSocketChannel connectInternal(boolean isReconnecting) throws Exception {
        if (!resources.isInitialized()) {
            throw new IllegalStateException("ResourcePool not initialized");
        }

        TlsMode mode = (cfg.security() != null && cfg.security().tlsMode() != null)
                ? cfg.security().tlsMode()
                : TlsMode.TOFU;

        String scheme = TlsResolver.schemeFor(mode);
        String host = cfg.endpoints().websocket().host();
        int port = cfg.endpoints().websocket().port();
        String url = scheme + "://" + host + ":" + port + "/ws";

        if (!isReconnecting) {
            Log.info("Connecting to {} as '{}'", url, cfg.clientId());
        }

        var builder = WebSocketClient.connectionBuilder(
                resources.getWorker(),
                resources.getBufferPool(),
                URI.create(url));

        if (resources.getSsl() != null) {
            builder.setSsl(resources.getSsl());
        }

        IoFuture<WebSocketChannel> ioFuture = builder.connect();
        CompletableFuture<WebSocketChannel> future = new CompletableFuture<>();

        ioFuture.addNotifier((ioResult, attachment) -> {
            switch (ioResult.getStatus()) {
                case DONE -> {
                    try {
                        future.complete(ioResult.get());
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                }
                case FAILED -> future.completeExceptionally(ioResult.getException());
                case CANCELLED -> future.cancel(true);
                default -> {
                    /* ignore */ }
            }
        }, null);

        WebSocketChannel channel = future.get(5, TimeUnit.SECONDS);
        channelRef.set(channel);

        return channel;
    }

    public WebSocketChannel getChannel() {
        return channelRef.get();
    }

    public boolean isConnected() {
        WebSocketChannel ch = channelRef.get();
        return ch != null && ch.isOpen();
    }

    public boolean isChannelHealthy() {
        WebSocketChannel ch = channelRef.get();
        if (ch == null || !ch.isOpen()) {
            return false;
        }
        if (ch.isCloseFrameSent() || ch.isCloseFrameReceived()) {
            return false;
        }
        return true;
    }

    public void disconnect(ConnectionState currentState) {
        WebSocketChannel ch = channelRef.getAndSet(null);
        if (ch == null) {
            return;
        }

        try {
            ch.suspendReceives();
        } catch (Throwable ignore) {
        }

        // only send close frame if authenticated and channel is healthy
        if (currentState.canSend() && ch.isOpen() && !ch.isCloseFrameSent() && !ch.isCloseFrameReceived()) {
            try {
                WebSockets.sendClose(
                        CloseMessage.NORMAL_CLOSURE,
                        "client closing",
                        ch,
                        new WebSocketCallback<>() {
                            @Override
                            public void complete(WebSocketChannel channel, Void context) {
                                IoUtils.safeClose(channel);
                            }

                            @Override
                            public void onError(WebSocketChannel channel, Void context, Throwable cause) {
                                IoUtils.safeClose(channel);
                            }
                        });
            } catch (Throwable t) {
                Log.debug("Failed to send close frame: {}", t.getMessage());
                IoUtils.safeClose(ch);
            }
        } else {
            IoUtils.safeClose(ch);
        }
    }

    public void forceClose() {
        WebSocketChannel ch = channelRef.getAndSet(null);
        if (ch != null) {
            try {
                ch.suspendReceives();
            } catch (Throwable ignore) {
            }

            IoUtils.safeClose(ch);

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
