package dev.objz.commandbridge.net;

import java.util.concurrent.CompletableFuture;

import dev.objz.commandbridge.net.proto.Envelope;

public interface Endpoint {
    CompletableFuture<Void> send(Envelope env);

    boolean isOpen();

    default String describe() {
        return "unknown";
    }
}
