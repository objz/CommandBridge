package dev.objz.commandbridge.net.endpoints;

import java.util.concurrent.CompletableFuture;

import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.proto.Envelope;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

public class WsEndpoint implements Endpoint {

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
        try {
            WebSockets.sendText(Envelope.MAPPER.writeValueAsString(env), ch, null);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            var cf = new CompletableFuture<Void>();
            cf.completeExceptionally(e);
            return cf;
        }
    }

}
