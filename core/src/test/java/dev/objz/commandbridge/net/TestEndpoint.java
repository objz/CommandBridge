package dev.objz.commandbridge.net;

import java.util.concurrent.CompletableFuture;

import dev.objz.commandbridge.net.proto.Envelope;

public class TestEndpoint implements Endpoint {
    private boolean open;

    public TestEndpoint() {
        this.open = true;
    }

    public TestEndpoint(boolean open) {
        this.open = open;
    }

    @Override
    public CompletableFuture<Void> send(Envelope env) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public String describe() {
        return "test-endpoint";
    }
}
