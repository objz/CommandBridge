package dev.objz.commandbridge.net.endpoints;

import java.util.concurrent.CompletableFuture;

import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.proto.Envelope;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

public final class WsEndpoint implements Endpoint {

    private final WebSocketChannel ch;

    public WsEndpoint(WebSocketChannel ch) {
        this.ch = ch;
    }

    public WebSocketChannel channel() {
        return ch;
    }

    @Override
    public boolean isOpen() {
        return ch != null && ch.isOpen();
    }

    @Override
    public String describe() {
        if (ch == null || ch.getSourceAddress() == null)
            return "unknown";
        return ch.getSourceAddress().toString();
    }

    @Override
    public CompletableFuture<Void> send(Envelope env) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            String text = Envelope.MAPPER.writeValueAsString(env);
            WebSockets.sendText(text, ch, new io.undertow.websockets.core.WebSocketCallback<Void>() {
                @Override
                public void complete(WebSocketChannel channel, Void context) {
                    future.complete(null);
                }

                @Override
                public void onError(WebSocketChannel channel, Void context, Throwable throwable) {
                    future.completeExceptionally(throwable);
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

}
